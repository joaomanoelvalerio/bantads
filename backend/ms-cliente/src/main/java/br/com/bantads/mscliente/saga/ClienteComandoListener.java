package br.com.bantads.mscliente.saga;

import br.com.bantads.mscliente.cliente.Cliente;
import br.com.bantads.mscliente.cliente.ClienteService;
import br.com.bantads.mscliente.config.RabbitMqConfig;
import br.com.bantads.mscliente.solicitacao.Solicitacao;
import br.com.bantads.mscliente.solicitacao.SolicitacaoService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * SAGA Aprovar Cliente (R9) — passos 1 e 4, os dois de responsabilidade do MS
 * Cliente (docs/specs/05-nao-funcionais/09-sagas-api-compositions.md).
 */
@Component
public class ClienteComandoListener {

    private static final Logger log = LoggerFactory.getLogger(ClienteComandoListener.class);

    private static final String TIPO_APROVAR = "cliente.aprovar-solicitacao";
    private static final String TIPO_REVERTER = "cliente.reverter-aprovacao";
    private static final String TIPO_MARCAR_NAO_APROVADA = "cliente.marcar-nao-aprovada";
    private static final String TIPO_CRIAR = "cliente.criar";
    private static final String TIPO_REMOVER = "cliente.remover";

    private final SolicitacaoService solicitacaoService;
    private final ClienteService clienteService;
    private final RabbitTemplate rabbitTemplate;

    public ClienteComandoListener(
            SolicitacaoService solicitacaoService, ClienteService clienteService, RabbitTemplate rabbitTemplate) {
        this.solicitacaoService = solicitacaoService;
        this.clienteService = clienteService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_COMANDO)
    public void aoReceberComando(ComandoSaga comando) {
        log.info("Comando SAGA recebido em ms.cliente.cmd: sagaId={} tipo={}", comando.getSagaId(), comando.getTipo());

        switch (comando.getTipo()) {
            case TIPO_APROVAR -> aprovar(comando);
            case TIPO_REVERTER -> reverter(comando);
            case TIPO_MARCAR_NAO_APROVADA -> marcarNaoAprovada(comando);
            case TIPO_CRIAR -> criar(comando);
            case TIPO_REMOVER -> remover(comando);
            default -> {
                log.warn("Tipo de comando desconhecido em ms.cliente.cmd: {}", comando.getTipo());
                responder(comando, "FALHA", null, "Tipo de comando desconhecido: " + comando.getTipo());
            }
        }
    }

    private void aprovar(ComandoSaga comando) {
        String cpf = texto(comando.getPayload(), "cpf");
        try {
            Optional<Solicitacao> aprovada = solicitacaoService.aprovarPendente(cpf);
            if (aprovada.isEmpty()) {
                responder(comando, "FALHA", null, "Solicitação Pendente não encontrada para o CPF " + cpf);
                return;
            }
            responder(comando, "SUCESSO", paraMapa(aprovada.get()), null);
        } catch (Exception e) {
            log.error("Falha ao aprovar solicitação para a saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao aprovar solicitação: " + e.getMessage());
        }
    }

    private void reverter(ComandoSaga comando) {
        String cpf = texto(comando.getPayload(), "cpf");
        try {
            solicitacaoService.reverterParaPendente(cpf);
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao reverter solicitação para Pendente na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao reverter solicitação: " + e.getMessage());
        }
    }

    private void marcarNaoAprovada(ComandoSaga comando) {
        String cpf = texto(comando.getPayload(), "cpf");
        String motivo = texto(comando.getPayload(), "motivo");
        try {
            solicitacaoService.marcarNaoAprovada(cpf, motivo);
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao marcar solicitação como Não aprovada na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao marcar solicitação: " + e.getMessage());
        }
    }

    private void criar(ComandoSaga comando) {
        Map<String, Object> dados = comando.getPayload();
        try {
            Cliente cliente = new Cliente();
            cliente.setCpf(texto(dados, "cpf"));
            cliente.setNome(texto(dados, "nome"));
            cliente.setEmail(texto(dados, "email"));
            cliente.setTelefone(texto(dados, "telefone"));
            cliente.setSalario(new BigDecimal(texto(dados, "salario")));
            cliente.setLogradouro(texto(dados, "logradouro"));
            cliente.setNumero(texto(dados, "numero"));
            cliente.setComplemento(texto(dados, "complemento"));
            cliente.setCep(texto(dados, "cep"));
            cliente.setCidade(texto(dados, "cidade"));
            cliente.setUf(texto(dados, "uf"));

            clienteService.criar(cliente);
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao criar cliente para a saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao criar cliente: " + e.getMessage());
        }
    }

    private void remover(ComandoSaga comando) {
        String cpf = texto(comando.getPayload(), "cpf");
        try {
            clienteService.remover(cpf);
            responder(comando, "SUCESSO", null, null);
        } catch (Exception e) {
            log.error("Falha ao remover cliente (compensação) na saga {}", comando.getSagaId(), e);
            responder(comando, "FALHA", null, "Falha ao remover cliente: " + e.getMessage());
        }
    }

    private Map<String, Object> paraMapa(Solicitacao solicitacao) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("cpf", solicitacao.getCpf());
        mapa.put("nome", solicitacao.getNome());
        mapa.put("email", solicitacao.getEmail());
        mapa.put("telefone", solicitacao.getTelefone());
        mapa.put("salario", solicitacao.getSalario().toPlainString());
        mapa.put("logradouro", solicitacao.getLogradouro());
        mapa.put("numero", solicitacao.getNumero());
        mapa.put("complemento", solicitacao.getComplemento());
        mapa.put("cep", solicitacao.getCep());
        mapa.put("cidade", solicitacao.getCidade());
        mapa.put("uf", solicitacao.getUf());
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
