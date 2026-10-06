package com.br.ms_email.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health -> 200 se o serviço estiver no ar.
 * Usado pelo healthcheck
 * do docker-compose.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
