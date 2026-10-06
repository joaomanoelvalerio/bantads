package br.com.bantads.msconta.reboot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RebootService {

    private final JdbcTemplate jdbcTemplate;
    private final Path seed;

    public RebootService(JdbcTemplate jdbcTemplate, @Value("${bantads.seed-sql:db/02-seed.sql}") String seed) {
        this.jdbcTemplate = jdbcTemplate;
        this.seed = Path.of(seed);
    }

    @Transactional
    public Map<String, Object> reboot() {
        jdbcTemplate.execute("TRUNCATE ms_conta.movimentacoes, ms_conta.contas, ms_conta.eventos_conta, "
                + "ms_conta.comandos_processados RESTART IDENTITY");
        jdbcTemplate.execute(lerSeed());
        return Map.of(
                "contas", jdbcTemplate.queryForObject("SELECT count(*) FROM ms_conta.contas", Integer.class),
                "eventos", jdbcTemplate.queryForObject("SELECT count(*) FROM ms_conta.eventos_conta", Integer.class));
    }

    private String lerSeed() {
        try {
            return Files.readString(seed);
        } catch (IOException e) {
            throw new IllegalStateException("Seed não encontrado em " + seed.toAbsolutePath(), e);
        }
    }
}
