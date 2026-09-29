package br.com.bantads.msauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Senhas com Argon2 (docs/specs/05-nao-funcionais/04-autenticacao.md).
 * Parâmetros default do Spring Security (`Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8`) —
 * razoáveis para este trabalho, sem tuning específico de hardware.
 */
@Configuration
public class SegurancaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
