package br.com.bantads.msconta;

import static br.com.bantads.msconta.config.RabbitMqConfig.FILA_EVENTOS;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.bantads.msconta.conta.Conta;
import br.com.bantads.msconta.conta.ContaRepository;
import br.com.bantads.msconta.conta.MovimentacaoRepository;
import br.com.bantads.msconta.evento.EventoConta;
import br.com.bantads.msconta.evento.EventoContaMensagem;
import br.com.bantads.msconta.evento.EventoContaService;
import br.com.bantads.msconta.evento.TipoEvento;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prova de ponta a ponta do pipeline de CQRS da Semana 04: append (command) -&gt;
 * publica em ms.conta.events (AMQP real, não mockado) -&gt; ProjecaoContaListener
 * consome e projeta no read model -&gt; reentrega da mesma mensagem não duplica o
 * efeito (idempotência, exigida pela entrega at-least-once do RabbitMQ — ver
 * docs/specs/05-nao-funcionais/10-cqrs.md).
 */
@SpringBootTest
class CqrsPipelineTest {

    private static final String CONTA_TESTE = "9001";

    @Autowired
    private EventoContaService eventoContaService;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void appendPublicaEProjetaDeFormaIdempotente() {
        eventoContaService.registrar(
                CONTA_TESTE, TipoEvento.CRIADO, Map.of("cpfCliente", "00000000000", "cpfGerente", "11111111111"));

        aguardar(() -> contaRepository.existsById(CONTA_TESTE), Duration.ofSeconds(10));
        Conta contaCriada = contaRepository.findById(CONTA_TESTE).orElseThrow();
        assertThat(contaCriada.getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);

        EventoConta deposito = eventoContaService.registrar(CONTA_TESTE, TipoEvento.DEPOSITO, Map.of("valor", "150.0000"));

        aguardar(() -> saldoAtual().compareTo(new BigDecimal("150.0000")) == 0, Duration.ofSeconds(10));
        assertThat(movimentacaoRepository.findAll().stream()
                        .anyMatch(m -> deposito.getId().equals(m.getEventoId())))
                .isTrue();

        // Reentrega manual da MESMA mensagem, simulando at-least-once do RabbitMQ.
        rabbitTemplate.convertAndSend(FILA_EVENTOS, EventoContaMensagem.de(deposito));
        dormir(Duration.ofSeconds(3));
        assertThat(saldoAtual()).isEqualByComparingTo(new BigDecimal("150.0000"));
    }

    private BigDecimal saldoAtual() {
        return contaRepository.findById(CONTA_TESTE).orElseThrow().getSaldo();
    }

    private void aguardar(Supplier<Boolean> condicao, Duration timeout) {
        long limite = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < limite) {
            if (Boolean.TRUE.equals(condicao.get())) {
                return;
            }
            dormir(Duration.ofMillis(200));
        }
        throw new AssertionError("Condição não satisfeita dentro do timeout de " + timeout);
    }

    private void dormir(Duration duracao) {
        try {
            Thread.sleep(duracao.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
