package com.br.orquestrador.saga.aprovarcliente;

import static com.br.orquestrador.OrquestradorApplication.QUEUE_MS_AUTH_CMD;
import static com.br.orquestrador.OrquestradorApplication.QUEUE_MS_CLIENTE_CMD;
import static com.br.orquestrador.OrquestradorApplication.QUEUE_MS_CONTA_CMD;
import static com.br.orquestrador.OrquestradorApplication.QUEUE_MS_EMAIL_CMD;
import static com.br.orquestrador.OrquestradorApplication.QUEUE_MS_GERENTE_CMD;

import com.br.orquestrador.saga.ComandoSagaPublicador;
import com.br.orquestrador.saga.JobRepositorio;
import com.br.orquestrador.saga.RespostaSaga;
import com.br.orquestrador.saga.SagaEstadoRepositorio;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SAGA 1 — Aprovar Cliente (R9), os 7 passos do enunciado. Roda inteira na
 * thread do listener de `saga.cmd` (uma SAGA de cada vez, sequencial) — mais
 * simples que um motor assíncrono de verdade, e suficiente pro volume de um
 * projeto de curso; documentado aqui em vez de escondido.
 *
 * <p>`etapaAtual` gravado no Redis é só para acompanhamento (console/depuração
 * na defesa) — quem decide o fluxo é este método, não uma releitura do
 * estado.
 */
@Service
public class AprovarClienteSagaService {

    private static final Logger log = LoggerFactory.getLogger(AprovarClienteSagaService.class);
    private static final Duration TIMEOUT_PASSO = Duration.ofSeconds(30);
    private static final String TIPO_SAGA = "aprovar-cliente";

    private final ComandoSagaPublicador publicador;
    private final JobRepositorio jobRepositorio;
    private final SagaEstadoRepositorio sagaEstadoRepositorio;

    public AprovarClienteSagaService(
            ComandoSagaPublicador publicador,
            JobRepositorio jobRepositorio,
            SagaEstadoRepositorio sagaEstadoRepositorio) {
        this.publicador = publicador;
        this.jobRepositorio = jobRepositorio;
        this.sagaEstadoRepositorio = sagaEstadoRepositorio;
    }

    public void executar(String sagaId, String cpf) {
        log.info("Iniciando SAGA Aprovar Cliente: sagaId={} cpf={}", sagaId, cpf);
        estado(sagaId, 0, "EM_ANDAMENTO", cpf);

        // Passo 1 — MS Cliente: aprova a solicitação, devolve os dados dela.
        RespostaSaga r1 = publicador.enviarEAguardar(
                QUEUE_MS_CLIENTE_CMD, sagaId, "cliente.aprovar-solicitacao", Map.of("cpf", cpf), TIMEOUT_PASSO);
        if (!r1.sucesso()) {
            log.warn("SAGA {} falhou no passo 1 (aprovar solicitação): {}", sagaId, r1.getErro());
            compensarPasso1(sagaId, cpf);
            estado(sagaId, 1, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(r1, "Não foi possível aprovar a solicitação"));
            return;
        }
        Map<String, Object> dadosSolicitacao = r1.getPayload();
        String email = texto(dadosSolicitacao, "email");
        String nome = texto(dadosSolicitacao, "nome");
        estado(sagaId, 1, "EM_ANDAMENTO", cpf);

        // Passo 2 — MS Gerente: lista gerentes ativos.
        RespostaSaga r2 = publicador.enviarEAguardar(
                QUEUE_MS_GERENTE_CMD, sagaId, "gerente.listar-ativos", Map.of(), TIMEOUT_PASSO);
        List<Map<String, Object>> gerentesAtivos = r2.sucesso() ? listaDe(r2.getPayload(), "gerentes") : List.of();
        if (!r2.sucesso() || gerentesAtivos.isEmpty()) {
            log.warn("SAGA {} falhou no passo 2 (listar gerentes ativos): {}", sagaId, r2.getErro());
            compensarPasso1(sagaId, cpf);
            falharComEmail(sagaId, cpf, email, mensagem(r2, "Não há gerente ativo disponível para atribuição"));
            return;
        }
        estado(sagaId, 2, "EM_ANDAMENTO", cpf);

        // Passo 3 — MS Conta: quantidade de clientes por gerente ativo.
        RespostaSaga r3 = publicador.enviarEAguardar(
                QUEUE_MS_CONTA_CMD, sagaId, "conta.contar-por-gerente", Map.of("gerentes", gerentesAtivos), TIMEOUT_PASSO);
        if (!r3.sucesso()) {
            log.warn("SAGA {} falhou no passo 3 (contar clientes por gerente): {}", sagaId, r3.getErro());
            compensarPasso1(sagaId, cpf);
            falharComEmail(sagaId, cpf, email, mensagem(r3, "Não foi possível apurar a carteira dos gerentes"));
            return;
        }
        String cpfGerenteEscolhido = escolherGerenteComMenosClientes(listaDe(r3.getPayload(), "gerentes"));
        estado(sagaId, 3, "EM_ANDAMENTO", cpf);

        // Passo 4 — MS Cliente: cria o dado de cliente definitivo (copiado da solicitação).
        RespostaSaga r4 = publicador.enviarEAguardar(
                QUEUE_MS_CLIENTE_CMD, sagaId, "cliente.criar", new LinkedHashMap<>(dadosSolicitacao), TIMEOUT_PASSO);
        if (!r4.sucesso()) {
            log.warn("SAGA {} falhou no passo 4 (criar cliente): {}", sagaId, r4.getErro());
            compensarPasso4(sagaId, cpf);
            compensarPasso1(sagaId, cpf);
            falharComEmail(sagaId, cpf, email, mensagem(r4, "Não foi possível cadastrar o cliente"));
            return;
        }
        estado(sagaId, 4, "EM_ANDAMENTO", cpf);

        // Passo 5 — MS Auth: cria a credencial com senha aleatória.
        RespostaSaga r5 = publicador.enviarEAguardar(
                QUEUE_MS_AUTH_CMD, sagaId, "auth.criar-credencial",
                Map.of("cpf", cpf, "login", email, "tipo", "CLIENTE"), TIMEOUT_PASSO);
        if (!r5.sucesso()) {
            boolean loginDuplicado = r5.getPayload() != null && "login_duplicado".equals(r5.getPayload().get("motivo"));
            log.warn("SAGA {} falhou no passo 5 (criar credencial); loginDuplicado={}", sagaId, loginDuplicado);
            compensarPasso5(sagaId, cpf);
            compensarPasso4(sagaId, cpf);
            if (loginDuplicado) {
                // Caso especial: NÃO devolve a Pendente — senão a mesma tentativa
                // falharia pra sempre.
                compensarPasso1ComoNaoAprovada(sagaId, cpf, "E-mail já cadastrado");
            } else {
                compensarPasso1(sagaId, cpf);
            }
            falharComEmail(sagaId, cpf, email, mensagem(r5, "Não foi possível gerar a credencial de acesso"));
            return;
        }
        // A senha em claro só existe nesta variável local e no payload do e-mail do passo 7 —
        // nunca gravada no estado da SAGA nem logada.
        String senhaEmClaro = texto(r5.getPayload(), "senha");
        estado(sagaId, 5, "EM_ANDAMENTO", cpf);

        // Passo 6 — MS Conta: cria a conta com número aleatório único, vinculada ao gerente escolhido.
        RespostaSaga r6 = publicador.enviarEAguardar(
                QUEUE_MS_CONTA_CMD, sagaId, "conta.criar",
                Map.of("cpfCliente", cpf, "cpfGerente", cpfGerenteEscolhido), TIMEOUT_PASSO);
        if (!r6.sucesso()) {
            log.warn("SAGA {} falhou no passo 6 (criar conta): {}", sagaId, r6.getErro());
            compensarPasso6(sagaId, cpf);
            compensarPasso5(sagaId, cpf);
            compensarPasso4(sagaId, cpf);
            compensarPasso1(sagaId, cpf);
            falharComEmail(sagaId, cpf, email, mensagem(r6, "Não foi possível criar a conta"));
            return;
        }
        estado(sagaId, 6, "EM_ANDAMENTO", cpf);

        // Passo 7 — MS Email: fire-and-forget, não bloqueia nem falha a SAGA.
        publicador.enviarSemAguardar(
                QUEUE_MS_EMAIL_CMD, sagaId, "email.enviar-senha", Map.of("email", email, "nome", nome, "senha", senhaEmClaro));

        estado(sagaId, 7, "SUCESSO", cpf);
        jobRepositorio.marcarConcluidoComoRecurso(sagaId, "clientes", cpf);
        log.info("SAGA {} concluída com sucesso para o cpf {}", sagaId, cpf);
    }

    private void falharComEmail(String sagaId, String cpf, String email, String motivo) {
        estado(sagaId, -1, "FALHA", cpf);
        if (email != null) {
            publicador.enviarSemAguardar(
                    QUEUE_MS_EMAIL_CMD, sagaId, "email.notificar-falha-solicitacao", Map.of("email", email, "motivo", motivo));
        }
        jobRepositorio.marcarFalha(sagaId, motivo);
    }

    private void compensarPasso1(String sagaId, String cpf) {
        publicador.enviarEAguardar(QUEUE_MS_CLIENTE_CMD, sagaId, "cliente.reverter-aprovacao", Map.of("cpf", cpf), TIMEOUT_PASSO);
    }

    private void compensarPasso1ComoNaoAprovada(String sagaId, String cpf, String motivo) {
        publicador.enviarEAguardar(
                QUEUE_MS_CLIENTE_CMD, sagaId, "cliente.marcar-nao-aprovada", Map.of("cpf", cpf, "motivo", motivo), TIMEOUT_PASSO);
    }

    private void compensarPasso6(String sagaId, String cpf) {
        publicador.enviarEAguardar(QUEUE_MS_CONTA_CMD, sagaId, "conta.remover", Map.of("cpfCliente", cpf), TIMEOUT_PASSO);
    }

    private void compensarPasso4(String sagaId, String cpf) {
        publicador.enviarEAguardar(QUEUE_MS_CLIENTE_CMD, sagaId, "cliente.remover", Map.of("cpf", cpf), TIMEOUT_PASSO);
    }

    private void compensarPasso5(String sagaId, String cpf) {
        publicador.enviarEAguardar(QUEUE_MS_AUTH_CMD, sagaId, "auth.remover-credencial", Map.of("cpf", cpf), TIMEOUT_PASSO);
    }

    /** Empate → qualquer um (R9) — a ordem de chegada da lista decide. */
    private String escolherGerenteComMenosClientes(List<Map<String, Object>> gerentesComContagem) {
        return gerentesComContagem.stream()
                .min((a, b) -> Long.compare(numero(a, "quantidadeClientes"), numero(b, "quantidadeClientes")))
                .map(gerente -> texto(gerente, "cpf"))
                .orElseThrow(() -> new IllegalStateException("Lista de gerentes com contagem veio vazia"));
    }

    private void estado(String sagaId, int etapa, String status, String cpf) {
        sagaEstadoRepositorio.salvar(sagaId, TIPO_SAGA, etapa, status, Map.of("cpf", cpf));
    }

    private String mensagem(RespostaSaga resposta, String padrao) {
        return resposta.getErro() != null ? resposta.getErro() : padrao;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listaDe(Map<String, Object> payload, String chave) {
        Object valor = payload == null ? null : payload.get(chave);
        return valor instanceof List<?> lista ? (List<Map<String, Object>>) lista : List.of();
    }

    private String texto(Map<String, Object> mapa, String chave) {
        Object valor = mapa == null ? null : mapa.get(chave);
        return valor == null ? null : String.valueOf(valor);
    }

    private long numero(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        return valor instanceof Number numero ? numero.longValue() : Long.MAX_VALUE;
    }
}
