package br.com.bantads.msauth.reboot;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RebootController {

    private final RebootService rebootService;

    public RebootController(RebootService rebootService) {
        this.rebootService = rebootService;
    }

    @PostMapping("/reboot")
    public Map<String, Object> reboot() {
        return Map.of("usuarios", rebootService.reboot());
    }
}
