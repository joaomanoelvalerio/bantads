package br.com.bantads.msgerente.reboot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public int reboot() {
        jdbcTemplate.execute("TRUNCATE ms_gerente.gerentes, ms_gerente.comandos_processados");
        jdbcTemplate.execute(lerSeed());
        return jdbcTemplate.queryForObject("SELECT count(*) FROM ms_gerente.gerentes", Integer.class);
    }

    private String lerSeed() {
        try {
            return Files.readString(seed);
        } catch (IOException e) {
            throw new IllegalStateException("Seed não encontrado em " + seed.toAbsolutePath(), e);
        }
    }
}
