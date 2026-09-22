package br.com.bantads.msconta;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.bantads.msconta.conta.ContaRepository;
import br.com.bantads.msconta.conta.MovimentacaoRepository;
import br.com.bantads.msconta.conta.OperacaoContaService;
import br.com.bantads.msconta.evento.EventoContaRepository;
import br.com.bantads.msconta.evento.EventoContaService;
import br.com.bantads.msconta.evento.TipoEvento;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Prova de ponta a ponta do exemplo que a própria especificação usa para
 * "optimistic locking resolve race condition" (docs/specs/05-nao-funcionais/10-cqrs.md):
 * 2 saques simultâneos na mesma conta, com saldo para só UM deles. Dispara os
 * dois de verdade em threads separadas (não sequencialmente) para que a
 * corrida na constraint unique(objeto_id, versao) realmente aconteça.
 */
@SpringBootTest
class OperacaoContaConcorrenciaTest {

    private static final String CONTA_TESTE = "9002";
    private static final String CPF_CLIENTE_TESTE = "22222222222";

    @Autowired
    private EventoContaService eventoContaService;

    @Autowired
    private OperacaoContaService operacaoContaService;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private EventoContaRepository eventoContaRepository;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    // Limpa a conta de teste antes de rodar, para o teste ser repetível contra
    // o mesmo Postgres persistente (docker-compose), não só numa base limpa.
    @BeforeEach
    void limparContaDeTeste() {
        movimentacaoRepository.deleteByNumeroConta(CONTA_TESTE);
        eventoContaRepository.deleteByObjetoId(CONTA_TESTE);
        contaRepository.deleteById(CONTA_TESTE);
    }

    @Test
    void doisSaquesSimultaneosComSaldoParaSoUmDeixamOSaldoConsistente() throws Exception {
        eventoContaService.registrar(
                CONTA_TESTE, TipoEvento.CRIADO, Map.of("cpfCliente", CPF_CLIENTE_TESTE, "cpfGerente", "11111111111"));
        aguardar(() -> contaRepository.existsById(CONTA_TESTE), Duration.ofSeconds(10));

        operacaoContaService.depositar(CONTA_TESTE, CPF_CLIENTE_TESTE, new BigDecimal("100.00"));
        aguardar(() -> saldoAtual().compareTo(new BigDecimal("100.00")) == 0, Duration.ofSeconds(10));

        CountDownLatch largada = new CountDownLatch(1);
        Callable<Boolean> tentativaDeSaque = () -> {
            largada.await();
            try {
                operacaoContaService.sacar(CONTA_TESTE, CPF_CLIENTE_TESTE, new BigDecimal("80.00"));
                return true; // sucesso
            } catch (ResponseStatusException saldoInsuficiente) {
                return false; // 422 — esperado para a operação perdedora
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> resultado1 = executor.submit(tentativaDeSaque);
            Future<Boolean> resultado2 = executor.submit(tentativaDeSaque);
            largada.countDown(); // solta as duas threads o mais junto possível

            List<Boolean> resultados = List.of(resultado1.get(), resultado2.get());

            // Duas tentativas de 80 num saldo de 100: só uma pode caber.
            assertThat(resultados).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdown();
        }

        aguardar(() -> saldoAtual().compareTo(new BigDecimal("20.00")) == 0, Duration.ofSeconds(10));
        assertThat(saldoAtual()).isEqualByComparingTo(new BigDecimal("20.00"));
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
            dormir(200);
        }
        throw new AssertionError("Condição não satisfeita dentro do timeout de " + timeout);
    }

    private void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
