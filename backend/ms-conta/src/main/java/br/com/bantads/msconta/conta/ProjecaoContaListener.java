package br.com.bantads.msconta.conta;

import br.com.bantads.msconta.config.RabbitMqConfig;
import br.com.bantads.msconta.evento.EventoContaMensagem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lado QUERY: consumidor de ms.conta.events, mantém ms_conta.contas /
 * ms_conta.movimentacoes em sincronia com o event store. Precisa ser
 * IDEMPOTENTE porque o RabbitMQ entrega at-least-once — usa
 * contas.ultima_versao_aplicada como guarda: eventos com versao já aplicada
 * são ignorados (docs/specs/05-nao-funcionais/10-cqrs.md).
 */
@Component
public class ProjecaoContaListener {

    private final ContaRepository contaRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final ObjectMapper objectMapper;

    public ProjecaoContaListener(
            ContaRepository contaRepository,
            MovimentacaoRepository movimentacaoRepository,
            ObjectMapper objectMapper) {
        this.contaRepository = contaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMqConfig.FILA_EVENTOS)
    @Transactional
    public void aoReceberEvento(EventoContaMensagem mensagem) {
        if (mensagem.tipo() == br.com.bantads.msconta.evento.TipoEvento.CRIADO) {
            aplicarCriacao(mensagem);
            return;
        }

        Conta conta = contaRepository.findById(mensagem.objetoId())
                .orElseThrow(() -> new IllegalStateException(
                        "Projeção recebeu evento para conta inexistente: " + mensagem.objetoId()
                                + " (o evento Criado ainda não foi projetado?)"));

        if (mensagem.versao() <= conta.getUltimaVersaoAplicada()) {
            return; // já aplicado — reentrega at-least-once do RabbitMQ
        }

        aplicarNaConta(conta, mensagem);
        conta.setUltimaVersaoAplicada(mensagem.versao());
        contaRepository.save(conta);
    }

    private void aplicarCriacao(EventoContaMensagem mensagem) {
        if (contaRepository.existsById(mensagem.objetoId())) {
            return; // já projetado
        }

        JsonNode payload = ler(mensagem);
        Conta conta = new Conta();
        conta.setNumeroConta(mensagem.objetoId());
        conta.setCpfCliente(payload.get("cpfCliente").asText());
        conta.setCpfGerente(payload.get("cpfGerente").asText());
        conta.setDataCriacao(mensagem.timestamp().toLocalDate());
        conta.setSaldo(BigDecimal.ZERO);
        conta.setUltimaVersaoAplicada(mensagem.versao());
        contaRepository.save(conta);
    }

    private void aplicarNaConta(Conta conta, EventoContaMensagem mensagem) {
        JsonNode payload = ler(mensagem);

        switch (mensagem.tipo()) {
            case DEPOSITO -> {
                BigDecimal valor = valor(payload);
                conta.setSaldo(conta.getSaldo().add(valor));
                registrarMovimentacao(conta, mensagem, TipoMovimentacao.DEPOSITO, valor, null, null, null, null);
            }
            case SAQUE -> {
                BigDecimal valor = valor(payload);
                conta.setSaldo(conta.getSaldo().subtract(valor));
                registrarMovimentacao(conta, mensagem, TipoMovimentacao.SAQUE, valor, null, null, null, null);
            }
            case TRANSFERENCIA_ORIGEM -> {
                // Payload deste lado só descreve a ponta oposta (destino) — ver
                // formato em EventoContaService/db/02-seed.sql. O nome do próprio
                // titular não é conhecido pelo MS Conta nesse evento; fica null aqui
                // (quem o injeta é o API Gateway ao enriquecer R6, S5).
                BigDecimal valor = valor(payload);
                conta.setSaldo(conta.getSaldo().subtract(valor));
                registrarMovimentacao(
                        conta,
                        mensagem,
                        TipoMovimentacao.TRANSFERENCIA,
                        valor,
                        conta.getCpfCliente(),
                        null,
                        payload.path("cpfDestino").asText(null),
                        payload.path("nomeDestino").asText(null));
            }
            case TRANSFERENCIA_DESTINO -> {
                // Simetricamente, este payload só descreve a ponta origem.
                BigDecimal valor = valor(payload);
                conta.setSaldo(conta.getSaldo().add(valor));
                registrarMovimentacao(
                        conta,
                        mensagem,
                        TipoMovimentacao.TRANSFERENCIA,
                        valor,
                        payload.path("cpfOrigem").asText(null),
                        payload.path("nomeOrigem").asText(null),
                        conta.getCpfCliente(),
                        null);
            }
            case GERENTE_ALTERADO -> conta.setCpfGerente(payload.get("cpfGerenteNovo").asText());
            case CRIADO -> throw new IllegalStateException("CRIADO é tratado em aplicarCriacao");
        }
    }

    private void registrarMovimentacao(
            Conta conta,
            EventoContaMensagem mensagem,
            TipoMovimentacao tipo,
            BigDecimal valor,
            String cpfOrigem,
            String nomeOrigem,
            String cpfDestino,
            String nomeDestino) {
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setNumeroConta(conta.getNumeroConta());
        movimentacao.setDataHora(mensagem.timestamp());
        movimentacao.setTipo(tipo);
        movimentacao.setCpfOrigem(cpfOrigem);
        movimentacao.setNomeOrigem(nomeOrigem);
        movimentacao.setCpfDestino(cpfDestino);
        movimentacao.setNomeDestino(nomeDestino);
        movimentacao.setValor(valor);
        movimentacao.setEventoId(mensagem.id());
        movimentacaoRepository.save(movimentacao);
    }

    private BigDecimal valor(JsonNode payload) {
        return new BigDecimal(payload.get("valor").asText());
    }

    private JsonNode ler(EventoContaMensagem mensagem) {
        try {
            return objectMapper.readTree(mensagem.payload());
        } catch (Exception e) {
            throw new IllegalStateException("payload inválido no evento id=" + mensagem.id(), e);
        }
    }
}
