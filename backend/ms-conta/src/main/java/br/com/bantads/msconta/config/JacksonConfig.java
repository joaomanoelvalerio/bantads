package br.com.bantads.msconta.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Valores monetários trafegam como string em todo payload JSON (nunca como number,
 * para evitar imprecisão de float) — ver docs/specs/05-nao-funcionais/01-tecnologias-padroes-dados.md.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> {
            SimpleModule modulo = new SimpleModule();
            modulo.addSerializer(BigDecimal.class, ToStringSerializer.instance);
            builder.modulesToInstall(modulo);
        };
    }
}
