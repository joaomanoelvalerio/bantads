package br.com.bantads.msgerente.saga;

import br.com.bantads.msgerente.config.RabbitMqConfig;
import br.com.bantads.msgerente.gerente.Gerente;
import br.com.bantads.msgerente.gerente.GerenteRepository;
import br.com.bantads.msgerente.gerente.GerenteService;
import br.com.bantads.msgerente.gerente.UltimoGerenteAtivoException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SAGA Aprovar Cliente (R9, passo 2), SAGA Inserir Gerente (R13, passo 1 +
 * compensação) e SAGA Remover Gerente (R15, passos 1/4 + compensação).
 */
@Component
public class GerenteComandoListener {

    private static final Logger log = LoggerFactory.getLogger(GerenteComandoListener.class);

    private static final String TIPO_LISTAR_ATIVOS = "gerente.listar-ativos";
    private static final String TIPO_INSERIR = "gerente.inserir";
    private static final String TIPO_REMOVER = "gerente.remover";
    private static final String TIPO_INATIVAR = "gerente.inativar";
    private static final String TIPO_REATIVAR = "gerente.reativar";

    private final GerenteService gerenteService;
    private final GerenteRepository gerenteRepository;
    private final ComandoProcessadoRepository comandoProcessadoRepository;
    private final RabbitTemplate rabbitTemplate;

    public GerenteComandoListener(
            GerenteService gerenteService,
            GerenteRepository gerenteRepository,
            ComandoProcessadoRepository comandoProcessadoRepository,
            RabbitTemplate rabbitTemplate) {
        this.gerenteService = gerenteService;
        this.gerenteRepository = gerenteRepository;
        this.comandoProcessadoRepository = comandoProcessadoRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_COMANDO)
    public void aoReceberComando(ComandoSaga comando) {
        log.info("Comando SAGA recebido em ms.gerente.cmd: sagaId={} tipo={}", comando.getSagaId(), comando.getTipo());

        switch (comando.getTipo()) {
            case TIPO_LISTAR_ATIVOS -> listarAtivos(comando);
            case TIPO_INSERIR -> inserir(comando);
            case TIPO_REMOVER -> remover(comando);
            case TIPO_INATIVAR -> inativar(comando);
            case TIPO_REATIVAR -> reativar(comando);
            default -> {
                log.warn("Tipo de comando desconhecido em ms.gerente.cmd: {}", comando.getTipo());
                responder(comando, "FALHA", null, "Tipo de comando desconhecido: " + comando.getTipo());
            }
        }
    }

    /** Passo de só-consulta (R9) — sem compensação. */
    private void listarAtivos(ComandoSaga comando) {
        try {
            List<Gerente> ativos = gerenteService.listarAtivos();
            List<Map<String, Object>> gerentes = ativos.stream().map(this::paraMapa).toList();
            responder(comando, "SUCESSO", Map.of("gerentes", gerentes), null);
        } catch (Exception e) {
            log.error("Falha ao listar gerentes ativos para a saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao listar gerentes ativos: " + e.getMessage());
        }
    }

    /**
     * SAGA Inserir Gerente (R13, passo 1). Idempotente por (sagaId, tipo) —
     * S8: `gerente.inserir`
     * não é idempotente por natureza (uma reentrega bateria no `UNIQUE` de
     * cpf/e-mail e pareceria um "cpf_ou_email_duplicado" de verdade, não uma
     * simples reentrega). Reentrega reconhecida só confirma SUCESSO de novo,
     * sem tentar inserir uma segunda vez.
     */
    private void inserir(ComandoSaga comando) {
        if (comandoProcessadoRepository.findByIdSagaIdAndIdTipo(comando.getSagaId(), comando.getTipo()).isPresent()) {
            log.info("gerente.inserir já processado antes para a saga {} — confirmando sucesso de novo", comando.getSagaId());
            responder(comando, "SUCESSO", null, null);
            return;
        }

        Map<String, Object> dados = comando.getPayload();
        try {
            Gerente gerente = new Gerente();
            gerente.setCpf(texto(dados, "cpf"));
            gerente.setNome(texto(dados, "nome"));
            gerente.setEmail(texto(dados, "email"));
            gerente.setTelefone(texto(dados, "telefone"));
            gerente.setAtivo(true);
            gerenteRepository.saveAndFlush(gerente);
            salvarComandoProcessado(comando);
            responder(comando, "SUCESSO", null, null);
        } catch (DataIntegrityViolationException e) {
            log.warn("CPF ou e-mail de gerente já cadastrado na saga {}", comando.getSagaId());
            responder(comando, "FALHA", Map.of("motivo", "cpf_ou_email_duplicado"), "CPF ou e-mail já cadastrado");
        } catch (Exception e) {
            log.error("Falha ao inserir gerente para a saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao inserir gerente: " + e.getMessage());
        }
    }

    private void salvarComandoProcessado(ComandoSaga comando) {
        try {
            comandoProcessadoRepository.save(new ComandoProcessado(comando.getSagaId(), comando.getTipo(), "{}"));
        } catch (DataIntegrityViolationException e) {
            log.warn("Corrida ao salvar o registro de idempotência da saga {} — outra entrega venceu", comando.getSagaId());
        }
    }

    /** Compensação do passo 1 — idempotente: não achar nada não é erro. */
    private void remover(ComandoSaga comando) {
        String cpf = texto(comando.getPayload(), "cpf");
        try {
            boolean inseridoNestaSaga =
                    comandoProcessadoRepository.findByIdSagaIdAndIdTipo(comando.getSagaId(), TIPO_INSERIR).isPresent();
            if (inseridoNestaSaga && gerenteRepository.existsById(cpf)) {
                gerenteRepository.deleteById(cpf);
            }
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao remover gerente (compensação) na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao remover gerente: " + e.getMessage());
        }
    }

    /** SAGA Remover Gerente (R15, passo 1). */
    private void inativar(ComandoSaga comando) {
        String cpf = texto(comando.getPayload(), "cpf");
        try {
            gerenteService.inativar(cpf);
            responder(comando, "SUCESSO", null, null);
        } catch (UltimoGerenteAtivoException e) {
            log.warn("Tentativa de remover o último gerente ativo na saga {}", comando.getSagaId());
            responder(comando, "FALHA", Map.of("motivo", "ultimo_gerente_ativo"), e.getMessage());
        } catch (Exception e) {
            log.error("Falha ao inativar gerente para a saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao inativar gerente: " + e.getMessage());
        }
    }

    /** Compensação do passo 1 — idempotente. */
    private void reativar(ComandoSaga comando) {
        String cpf = texto(comando.getPayload(), "cpf");
        try {
            gerenteService.reativar(cpf);
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao reativar gerente (compensação) na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao reativar gerente: " + e.getMessage());
        }
    }

    private Map<String, Object> paraMapa(Gerente gerente) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("cpf", gerente.getCpf());
        mapa.put("nome", gerente.getNome());
        return mapa;
    }

    private String texto(Map<String, Object> payload, String chave) {
        Object valor = payload.get(chave);
        return valor == null ? null : String.valueOf(valor);
    }

    private void responder(ComandoSaga comando, String status, Map<String, Object> payload, String erro) {
        RespostaSaga resposta = new RespostaSaga(
                comando.getSagaId(), comando.getTipo(), payload, OffsetDateTime.now().toString(), status, erro);
        rabbitTemplate.convertAndSend(RabbitMqConfig.FILA_RESPOSTA, resposta);
    }
}
