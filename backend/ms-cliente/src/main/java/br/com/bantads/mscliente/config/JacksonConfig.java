package br.com.bantads.mscliente.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Valores monetários trafegam como string em todo payload JSON (nunca como number,
 * para evitar imprecisão de float) — sempre com exatamente 2 casas decimais,
 * mesmo vindo do NUMERIC(19,4) do Postgres (contrato da API usa 2 casas, não 4).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> {
            SimpleModule modulo = new SimpleModule();
            modulo.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
                @Override
                public void serialize(BigDecimal valor, JsonGenerator gerador, SerializerProvider provedor)
                        throws IOException {
                    gerador.writeString(valor.setScale(2, RoundingMode.HALF_UP).toPlainString());
                }
            });
            builder.modulesToInstall(modulo);
        };
    }
}
