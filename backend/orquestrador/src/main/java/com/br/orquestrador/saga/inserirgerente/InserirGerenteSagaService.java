package com.br.orquestrador.saga.inserirgerente;

import static com.br.orquestrador.OrquestradorApplication.QUEUE_MS_AUTH_CMD;
import static com.br.orquestrador.OrquestradorApplication.QUEUE_MS_CONTA_CMD;
import static com.br.orquestrador.OrquestradorApplication.QUEUE_MS_EMAIL_CMD;
import static com.br.orquestrador.OrquestradorApplication.QUEUE_MS_GERENTE_CMD;

import com.br.orquestrador.saga.ComandoSagaPublicador;
import com.br.orquestrador.saga.ConsultaRestClient;
import com.br.orquestrador.saga.JobRepositorio;
import com.br.orquestrador.saga.RespostaSaga;
import com.br.orquestrador.saga.SagaEstadoRepositorio;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SAGA 2 — Inserção de Gerente (R13),
 * docs/specs/05-nao-funcionais/09-sagas-api-compositions.md. Mesmo desenho
 * sequencial/bloqueante de {@code AprovarClienteSagaService} — ver o
 * Javadoc de lá pra justificativa. Passos 4/5/6 são condicionais: só rodam
 * se o passo 3 encontrar uma conta pra transferir.
 */
@Service
public class InserirGerenteSagaService {

    private static final Logger log = LoggerFactory.getLogger(InserirGerenteSagaService.class);
    private static final Duration TIMEOUT_PASSO = Duration.ofSeconds(30);
    private static final String TIPO_SAGA = "inserir-gerente";

    private final ComandoSagaPublicador publicador;
    private final JobRepositorio jobRepositorio;
    private final SagaEstadoRepositorio sagaEstadoRepositorio;
    private final ConsultaRestClient consultaRestClient;

    public InserirGerenteSagaService(
            ComandoSagaPublicador publicador,
            JobRepositorio jobRepositorio,
            SagaEstadoRepositorio sagaEstadoRepositorio,
            ConsultaRestClient consultaRestClient) {
        this.publicador = publicador;
        this.jobRepositorio = jobRepositorio;
        this.sagaEstadoRepositorio = sagaEstadoRepositorio;
        this.consultaRestClient = consultaRestClient;
    }

    public void executar(String sagaId, Map<String, Object> dadosNovoGerente) {
        String cpf = texto(dadosNovoGerente, "cpf");
        String nome = texto(dadosNovoGerente, "nome");
        String email = texto(dadosNovoGerente, "email");
        String telefone = texto(dadosNovoGerente, "telefone");
        String senha = texto(dadosNovoGerente, "senha");

        log.info("Iniciando SAGA Inserir Gerente: sagaId={} cpf={}", sagaId, cpf);
        estado(sagaId, 0, "EM_ANDAMENTO", cpf);

        // Snapshot dos gerentes ativos ANTES de inserir o novo — o passo 3 usa
        // esta lista como candidatos a doar uma conta; se fosse lida depois do
        // passo 1, o próprio gerente recém-criado apareceria nela.
        RespostaSaga rListar = publicador.enviarEAguardar(
                QUEUE_MS_GERENTE_CMD, sagaId, "gerente.listar-ativos", Map.of(), TIMEOUT_PASSO);
        if (!rListar.sucesso()) {
            log.warn("SAGA {} falhou consultando gerentes ativos: {}", sagaId, rListar.getErro());
            estado(sagaId, 0, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(rListar, "Não foi possível consultar os gerentes ativos"));
            return;
        }
        List<String> cpfsCandidatos = listaDe(rListar.getPayload(), "gerentes").stream()
                .map(gerente -> texto(gerente, "cpf"))
                .toList();

        // Passo 1 — MS Gerente: insere o gerente.
        RespostaSaga r1 = publicador.enviarEAguardar(
                QUEUE_MS_GERENTE_CMD, sagaId, "gerente.inserir",
                Map.of("cpf", cpf, "nome", nome, "email", email, "telefone", telefone), TIMEOUT_PASSO);
        if (!r1.sucesso()) {
            log.warn("SAGA {} falhou no passo 1 (inserir gerente): {}", sagaId, r1.getErro());
            estado(sagaId, 1, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(r1, "Não foi possível cadastrar o gerente"));
            return;
        }
        estado(sagaId, 1, "EM_ANDAMENTO", cpf);

        // Passo 2 — MS Auth: cria autenticação com a senha informada no formulário.
        RespostaSaga r2 = publicador.enviarEAguardar(
                QUEUE_MS_AUTH_CMD, sagaId, "auth.criar-credencial",
                Map.of("cpf", cpf, "login", email, "tipo", "GERENTE", "senha", senha), TIMEOUT_PASSO);
        if (!r2.sucesso()) {
            log.warn("SAGA {} falhou no passo 2 (criar credencial): {}", sagaId, r2.getErro());
            compensarPasso1(sagaId, cpf);
            estado(sagaId, 2, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(r2, "Não foi possível gerar a credencial de acesso"));
            return;
        }
        estado(sagaId, 2, "EM_ANDAMENTO", cpf);

        // Passo 3 — MS Conta: identifica a conta a transferir (condicional: pode não haver nenhuma).
        RespostaSaga r3 = publicador.enviarEAguardar(
                QUEUE_MS_CONTA_CMD, sagaId, "conta.identificar-transferencia", Map.of("gerentes", cpfsCandidatos), TIMEOUT_PASSO);
        if (!r3.sucesso()) {
            log.warn("SAGA {} falhou no passo 3 (identificar transferência): {}", sagaId, r3.getErro());
            compensarPasso2(sagaId, cpf);
            compensarPasso1(sagaId, cpf);
            estado(sagaId, 3, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(r3, "Não foi possível identificar conta para transferência"));
            return;
        }

        boolean semConta = Boolean.TRUE.equals(r3.getPayload().get("semConta"));
        if (semConta) {
            // Passos 4/5/6 pulados — gerente criado sem contas, SAGA termina em sucesso
            // (docs/specs/02-requisitos-funcionais.md, R13).
            estado(sagaId, 6, "SUCESSO", cpf);
            jobRepositorio.marcarConcluidoComoRecurso(sagaId, "gerentes", cpf);
            log.info("SAGA {} concluída com sucesso (sem conta a transferir) para o cpf {}", sagaId, cpf);
            return;
        }

        String numeroConta = texto(r3.getPayload(), "numeroConta");
        String cpfGerenteOrigem = texto(r3.getPayload(), "cpfGerenteOrigem");
        estado(sagaId, 3, "EM_ANDAMENTO", cpf);

        // Passo 4 — MS Conta: atribui a conta ao novo gerente.
        RespostaSaga r4 = publicador.enviarEAguardar(
                QUEUE_MS_CONTA_CMD, sagaId, "conta.atribuir-gerente",
                Map.of("numeroConta", numeroConta, "cpfGerente", cpf), TIMEOUT_PASSO);
        if (!r4.sucesso()) {
            log.warn("SAGA {} falhou no passo 4 (atribuir conta): {}", sagaId, r4.getErro());
            compensarPasso2(sagaId, cpf);
            compensarPasso1(sagaId, cpf);
            estado(sagaId, 4, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(r4, "Não foi possível atribuir a conta ao novo gerente"));
            return;
        }
        estado(sagaId, 4, "EM_ANDAMENTO", cpf);

        // Passo 5 — MS Cliente: nome/e-mail do dono da conta transferida (consulta REST direta, sem compensação).
        String[] dadosCliente = buscarNomeEEmailDoCliente(numeroConta);
        estado(sagaId, 5, "EM_ANDAMENTO", cpf);

        // Passo 6 — MS Email: fire-and-forget; se a consulta do passo 5 falhar, só não manda o aviso.
        if (dadosCliente != null) {
            publicador.enviarSemAguardar(
                    QUEUE_MS_EMAIL_CMD, sagaId, "email.notificar-troca-gerente",
                    Map.of("email", dadosCliente[1], "nome", dadosCliente[0], "nomeGerenteNovo", nome));
        } else {
            log.warn("SAGA {} não conseguiu resolver o cliente da conta {} para notificar a troca de gerente", sagaId, numeroConta);
        }

        estado(sagaId, 6, "SUCESSO", cpf);
        jobRepositorio.marcarConcluidoComoRecurso(sagaId, "gerentes", cpf);
        log.info("SAGA {} concluída com sucesso para o cpf {} (conta {} transferida de {})", sagaId, cpf, numeroConta, cpfGerenteOrigem);
    }

    /** {nome, email} do cliente dono da conta, ou null se a consulta falhar em qualquer ponta. */
    private String[] buscarNomeEEmailDoCliente(String numeroConta) {
        return consultaRestClient.buscarConta(numeroConta)
                .map(conta -> String.valueOf(conta.get("cpfCliente")))
                .flatMap(consultaRestClient::buscarCliente)
                .map(cliente -> new String[] {String.valueOf(cliente.get("nome")), String.valueOf(cliente.get("email"))})
                .orElse(null);
    }

    private void compensarPasso1(String sagaId, String cpf) {
        publicador.enviarEAguardar(QUEUE_MS_GERENTE_CMD, sagaId, "gerente.remover", Map.of("cpf", cpf), TIMEOUT_PASSO);
    }

    private void compensarPasso2(String sagaId, String cpf) {
        publicador.enviarEAguardar(QUEUE_MS_AUTH_CMD, sagaId, "auth.remover-credencial", Map.of("cpf", cpf), TIMEOUT_PASSO);
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
}
