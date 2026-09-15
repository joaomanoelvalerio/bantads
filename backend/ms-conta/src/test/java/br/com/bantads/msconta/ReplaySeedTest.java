package br.com.bantads.msconta;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.bantads.msconta.conta.SaldoReplayService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prova automatizada do marco da Semana 04: "replay do seed reproduz
 * exatamente os saldos da tabela do enunciado" (docs/specs/10-cronograma.md).
 * Os valores esperados vêm de docs/specs/05-dados-pre-cadastrados.md, já
 * reconciliados manualmente em docs/design/modelagem-ct.md. Roda contra o
 * Postgres real (seed aplicado pelos scripts em db/) — ver README de como
 * executar via Docker.
 */
@SpringBootTest
class ReplaySeedTest {

    @Autowired
    private SaldoReplayService saldoReplayService;

    @Test
    void replayReproduzSaldosDoSeed() {
        assertThat(saldoReplayService.replaySaldo("1291")).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(saldoReplayService.replaySaldo("0950")).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(saldoReplayService.replaySaldo("8573")).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(saldoReplayService.replaySaldo("5887")).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(saldoReplayService.replaySaldo("7617")).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void replayDeContaSemEventosEhZero() {
        assertThat(saldoReplayService.replaySaldo("0000")).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
