package br.com.bantads.msauth.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /health -&gt; 200 se o serviço estiver no ar
 * (docs/specs/05-nao-funcionais/11-health-reboot.md).
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
