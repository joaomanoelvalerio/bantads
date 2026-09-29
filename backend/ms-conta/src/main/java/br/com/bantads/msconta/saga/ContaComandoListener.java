package br.com.bantads.msconta.saga;

import br.com.bantads.msconta.conta.Conta;
import br.com.bantads.msconta.conta.ContaRepository;
import br.com.bantads.msconta.conta.ContaService;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * SAGA Aprovar Cliente (R9, passos 3/6) e SAGA Inserir Gerente (R13, passos
 * 3/4 + compensação) — docs/specs/05-nao-funcionais/09-sagas-api-compositions.md.
 */
@Component
public class ContaComandoListener {

    private static final Logger log = LoggerFactory.getLogger(ContaComandoListener.class);

    private static final String TIPO_CONTAR_POR_GERENTE = "conta.contar-por-gerente";
    private static final String TIPO_CRIAR = "conta.criar";
    private static final String TIPO_REMOVER = "conta.remover";
    private static final String TIPO_IDENTIFICAR_TRANSFERENCIA = "conta.identificar-transferencia";
    private static final String TIPO_ATRIBUIR_GERENTE = "conta.atribuir-gerente";

    private final ContaService contaService;
    private final ContaRepository contaRepository;
    private final RabbitTemplate rabbitTemplate;

    public ContaComandoListener(ContaService contaService, ContaRepository contaRepository, RabbitTemplate rabbitTemplate) {
        this.contaService = contaService;
        this.contaRepository = contaRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMqConfigSaga.FILA_COMANDO)
    public void aoReceberComando(ComandoSaga comando) {
        log.info("Comando SAGA recebido em ms.conta.cmd: sagaId={} tipo={}", comando.getSagaId(), comando.getTipo());

        switch (comando.getTipo()) {
            case TIPO_CONTAR_POR_GERENTE -> contarPorGerente(comando);
            case TIPO_CRIAR -> criar(comando);
            case TIPO_REMOVER -> remover(comando);
            case TIPO_IDENTIFICAR_TRANSFERENCIA -> identificarTransferencia(comando);
            case TIPO_ATRIBUIR_GERENTE -> atribuirGerente(comando);
            default -> {
                log.warn("Tipo de comando desconhecido em ms.conta.cmd: {}", comando.getTipo());
                responder(comando, "FALHA", null, "Tipo de comando desconhecido: " + comando.getTipo());
            }
        }
    }

    /** Passo 3 — só consulta, sem compensação: devolve a contagem, quem decide o "menos clientes" é o Orquestrador. */
    @SuppressWarnings("unchecked")
    private void contarPorGerente(ComandoSaga comando) {
        try {
            List<Map<String, Object>> gerentesRecebidos = (List<Map<String, Object>>) comando.getPayload().get("gerentes");
            List<Map<String, Object>> comContagem = gerentesRecebidos.stream().map(gerente -> {
                String cpf = String.valueOf(gerente.get("cpf"));
                Map<String, Object> item = new LinkedHashMap<>(gerente);
                item.put("quantidadeClientes", contaRepository.countByCpfGerente(cpf));
                return item;
            }).toList();

            responder(comando, "SUCESSO", Map.of("gerentes", comContagem), null);
        } catch (Exception e) {
            log.error("Falha ao contar contas por gerente na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao contar contas por gerente: " + e.getMessage());
        }
    }

    private void criar(ComandoSaga comando) {
        String cpfCliente = String.valueOf(comando.getPayload().get("cpfCliente"));
        String cpfGerente = String.valueOf(comando.getPayload().get("cpfGerente"));
        try {
            Conta conta = contaService.criarConta(cpfCliente, cpfGerente);
            responder(comando, "SUCESSO", Map.of("numeroConta", conta.getNumeroConta()), null);
        } catch (Exception e) {
            log.error("Falha ao criar conta para a saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao criar conta: " + e.getMessage());
        }
    }

    private void remover(ComandoSaga comando) {
        String numeroConta = String.valueOf(comando.getPayload().get("numeroConta"));
        try {
            contaService.remover(numeroConta);
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao remover conta (compensação) na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao remover conta: " + e.getMessage());
        }
    }

    /** SAGA Inserir Gerente (R13, passo 3) — só consulta, sem compensação. */
    @SuppressWarnings("unchecked")
    private void identificarTransferencia(ComandoSaga comando) {
        try {
            List<String> cpfsGerentesAtivos = (List<String>) comando.getPayload().get("gerentes");
            Optional<Conta> escolhida = contaService.identificarContaParaTransferir(cpfsGerentesAtivos);

            if (escolhida.isEmpty()) {
                responder(comando, "SUCESSO", Map.of("semConta", true), null);
                return;
            }

            Conta conta = escolhida.get();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("semConta", false);
            payload.put("numeroConta", conta.getNumeroConta());
            payload.put("cpfGerenteOrigem", conta.getCpfGerente());
            responder(comando, "SUCESSO", payload, null);
        } catch (Exception e) {
            log.error("Falha ao identificar conta para transferência na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao identificar conta para transferência: " + e.getMessage());
        }
    }

    /**
     * SAGA Inserir Gerente (R13, passo 4) — também usado como a própria
     * compensação desse passo, chamado de novo com o `cpfGerente` original.
     */
    private void atribuirGerente(ComandoSaga comando) {
        String numeroConta = String.valueOf(comando.getPayload().get("numeroConta"));
        String cpfGerente = String.valueOf(comando.getPayload().get("cpfGerente"));
        try {
            contaService.atribuirGerente(numeroConta, cpfGerente);
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao atribuir gerente à conta na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao atribuir gerente à conta: " + e.getMessage());
        }
    }

    private void responder(ComandoSaga comando, String status, Map<String, Object> payload, String erro) {
        RespostaSaga resposta = new RespostaSaga(
                comando.getSagaId(), comando.getTipo(), payload, OffsetDateTime.now().toString(), status, erro);
        rabbitTemplate.convertAndSend(RabbitMqConfigSaga.FILA_RESPOSTA, resposta);
    }
}
