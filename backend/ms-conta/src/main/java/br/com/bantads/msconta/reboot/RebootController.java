package br.com.bantads.msconta.reboot;

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
        return rebootService.reboot();
    }
}
