package com.br.orquestrador.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health -> 200 se o serviço estiver no ar
 * (docs/specs/05-nao-funcionais/11-health-reboot.md). Usado pelo healthcheck
 * do docker-compose.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
