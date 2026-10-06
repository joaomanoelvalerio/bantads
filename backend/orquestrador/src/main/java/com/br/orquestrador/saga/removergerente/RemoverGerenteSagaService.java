package com.br.orquestrador.saga.removergerente;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SAGA 3 — Remoção de Gerente (R15). Mesmo desenho
 * sequencial/bloqueante das outras duas SAGAs (ver Javadoc de
 * {@code AprovarClienteSagaService}). O passo 3 (logout forçado) não usa
 * RabbitMQ — é acesso direto ao Redis, sem compensação por desenho: "se a
 * SAGA falhar, o gerente reativado faz novo login". Os passos 4/5/6/7 só
 * rodam se o gerente removido tinha alguma conta — sem contas, a SAGA
 * termina em sucesso direto depois do passo 3.
 */
@Service
public class RemoverGerenteSagaService {

    private static final Logger log = LoggerFactory.getLogger(RemoverGerenteSagaService.class);
    private static final Duration TIMEOUT_PASSO = Duration.ofSeconds(30);
    private static final String TIPO_SAGA = "remover-gerente";

    private final ComandoSagaPublicador publicador;
    private final JobRepositorio jobRepositorio;
    private final SagaEstadoRepositorio sagaEstadoRepositorio;
    private final SessaoRedisService sessaoRedisService;
    private final ConsultaRestClient consultaRestClient;

    public RemoverGerenteSagaService(
            ComandoSagaPublicador publicador,
            JobRepositorio jobRepositorio,
            SagaEstadoRepositorio sagaEstadoRepositorio,
            SessaoRedisService sessaoRedisService,
            ConsultaRestClient consultaRestClient) {
        this.publicador = publicador;
        this.jobRepositorio = jobRepositorio;
        this.sagaEstadoRepositorio = sagaEstadoRepositorio;
        this.sessaoRedisService = sessaoRedisService;
        this.consultaRestClient = consultaRestClient;
    }

    public void executar(String sagaId, String cpf) {
        log.info("Iniciando SAGA Remover Gerente: sagaId={} cpf={}", sagaId, cpf);
        estado(sagaId, 0, "EM_ANDAMENTO", cpf);

        // Passo 1 — MS Gerente: seta Inativo (recusa se for o último ativo).
        RespostaSaga r1 = publicador.enviarEAguardar(
                QUEUE_MS_GERENTE_CMD, sagaId, "gerente.inativar", Map.of("cpf", cpf), TIMEOUT_PASSO);
        if (!r1.sucesso()) {
            log.warn("SAGA {} falhou no passo 1 (inativar gerente): {}", sagaId, r1.getErro());
            compensarPasso1(sagaId, cpf);
            estado(sagaId, 1, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(r1, "Não foi possível inativar o gerente"));
            return;
        }
        estado(sagaId, 1, "EM_ANDAMENTO", cpf);

        // Passo 2 — MS Auth: desativa a credencial.
        RespostaSaga r2 = publicador.enviarEAguardar(
                QUEUE_MS_AUTH_CMD, sagaId, "auth.desativar-credencial", Map.of("cpf", cpf), TIMEOUT_PASSO);
        if (!r2.sucesso()) {
            log.warn("SAGA {} falhou no passo 2 (desativar credencial): {}", sagaId, r2.getErro());
            compensarPasso2(sagaId, cpf);
            compensarPasso1(sagaId, cpf);
            estado(sagaId, 2, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(r2, "Não foi possível desativar a credencial"));
            return;
        }
        estado(sagaId, 2, "EM_ANDAMENTO", cpf);

        // Passo 3 — Orquestrador: apaga a sessão do Redis, logout forçado. Sem RabbitMQ, sem compensação.
        try {
            sessaoRedisService.encerrarSessaoDoGerente(cpf);
        } catch (Exception e) {
            log.warn("Falha ao apagar a sessão Redis do gerente {} na saga {} — sem compensação pra este passo, a SAGA segue", cpf, sagaId, e);
        }
        estado(sagaId, 3, "EM_ANDAMENTO", cpf);

        // Passo 4 — MS Gerente: lista os ativos restantes (candidatos a herdar os clientes do removido).
        RespostaSaga r4 = publicador.enviarEAguardar(
                QUEUE_MS_GERENTE_CMD, sagaId, "gerente.listar-ativos", Map.of(), TIMEOUT_PASSO);
        if (!r4.sucesso()) {
            log.warn("SAGA {} falhou no passo 4 (listar gerentes ativos): {}", sagaId, r4.getErro());
            compensarPasso2(sagaId, cpf);
            compensarPasso1(sagaId, cpf);
            estado(sagaId, 4, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(r4, "Não foi possível consultar os gerentes ativos"));
            return;
        }
        List<Map<String, Object>> gerentesAtivos = listaDe(r4.getPayload(), "gerentes");
        List<String> cpfsGerentesAtivos = gerentesAtivos.stream().map(g -> texto(g, "cpf")).toList();
        estado(sagaId, 4, "EM_ANDAMENTO", cpf);

        // Passo 5 — MS Conta: transfere todas as contas do gerente removido pro ativo com menos contas.
        RespostaSaga r5 = publicador.enviarEAguardar(
                QUEUE_MS_CONTA_CMD, sagaId, "conta.transferir-todas-do-gerente",
                Map.of("cpfGerenteOrigem", cpf, "gerentesAtivos", cpfsGerentesAtivos), TIMEOUT_PASSO);
        if (!r5.sucesso()) {
            log.warn("SAGA {} falhou no passo 5 (transferir contas): {}", sagaId, r5.getErro());
            compensarPasso5(sagaId, cpf);
            compensarPasso2(sagaId, cpf);
            compensarPasso1(sagaId, cpf);
            estado(sagaId, 5, "FALHA", cpf);
            jobRepositorio.marcarFalha(sagaId, mensagem(r5, "Não foi possível transferir as contas do gerente"));
            return;
        }

        boolean semContas = Boolean.TRUE.equals(r5.getPayload().get("semContas"));
        if (semContas) {
            // Gerente removido não tinha clientes — passos 6/7 pulados, SAGA termina em sucesso.
            estado(sagaId, 7, "SUCESSO", cpf);
            jobRepositorio.marcarConcluidoInline(sagaId, Map.of(
                    "mensagem", "Gerente removido com sucesso. Ele não tinha clientes para transferir."));
            log.info("SAGA {} concluída com sucesso (sem contas a transferir) para o cpf {}", sagaId, cpf);
            return;
        }

        List<String> numerosConta = listaDeStrings(r5.getPayload(), "numerosConta");
        List<String> cpfsClientes = listaDeStrings(r5.getPayload(), "cpfsClientes");
        String cpfGerenteDestino = texto(r5.getPayload(), "cpfGerenteDestino");
        String nomeGerenteDestino = gerentesAtivos.stream()
                .filter(g -> cpfGerenteDestino.equals(texto(g, "cpf")))
                .map(g -> texto(g, "nome"))
                .findFirst()
                .orElse("um novo gerente");
        estado(sagaId, 5, "EM_ANDAMENTO", cpf);

        // Passo 6 — MS Cliente: nome/e-mail de cada cliente das contas transferidas (consulta REST direta, sem compensação).
        List<String[]> clientes = new ArrayList<>();
        for (String cpfCliente : cpfsClientes) {
            consultaRestClient.buscarCliente(cpfCliente).ifPresentOrElse(
                    dados -> clientes.add(new String[] {String.valueOf(dados.get("nome")), String.valueOf(dados.get("email"))}),
                    () -> log.warn("Não foi possível resolver nome/e-mail do cliente {} na saga {}", cpfCliente, sagaId));
        }
        estado(sagaId, 6, "EM_ANDAMENTO", cpf);

        // Passo 7 — MS Email: avisa todos os clientes afetados, fire-and-forget.
        for (String[] cliente : clientes) {
            publicador.enviarSemAguardar(
                    QUEUE_MS_EMAIL_CMD, sagaId, "email.notificar-troca-gerente",
                    Map.of("email", cliente[1], "nome", cliente[0], "nomeGerenteNovo", nomeGerenteDestino));
        }

        estado(sagaId, 7, "SUCESSO", cpf);
        jobRepositorio.marcarConcluidoInline(sagaId, Map.of(
                "mensagem", "Gerente removido com sucesso. " + numerosConta.size()
                        + " conta(s) transferida(s) para " + nomeGerenteDestino + "."));
        log.info("SAGA {} concluída com sucesso para o cpf {} ({} conta(s) transferida(s) para {})",
                sagaId, cpf, numerosConta.size(), cpfGerenteDestino);
    }

    private void compensarPasso5(String sagaId, String cpf) {
        publicador.enviarEAguardar(
                QUEUE_MS_CONTA_CMD, sagaId, "conta.reverter-transferencia-do-gerente",
                Map.of("cpfGerenteOrigem", cpf), TIMEOUT_PASSO);
    }

    private void compensarPasso1(String sagaId, String cpf) {
        publicador.enviarEAguardar(QUEUE_MS_GERENTE_CMD, sagaId, "gerente.reativar", Map.of("cpf", cpf), TIMEOUT_PASSO);
    }

    private void compensarPasso2(String sagaId, String cpf) {
        publicador.enviarEAguardar(QUEUE_MS_AUTH_CMD, sagaId, "auth.reativar-credencial", Map.of("cpf", cpf), TIMEOUT_PASSO);
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

    @SuppressWarnings("unchecked")
    private List<String> listaDeStrings(Map<String, Object> payload, String chave) {
        Object valor = payload == null ? null : payload.get(chave);
        if (!(valor instanceof List<?> lista)) {
            return List.of();
        }
        return ((List<Object>) lista).stream().map(String::valueOf).toList();
    }

    private String texto(Map<String, Object> mapa, String chave) {
        Object valor = mapa == null ? null : mapa.get(chave);
        return valor == null ? null : String.valueOf(valor);
    }
}
