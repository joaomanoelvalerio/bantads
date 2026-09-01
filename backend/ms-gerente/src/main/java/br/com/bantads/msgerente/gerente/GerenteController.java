package br.com.bantads.msgerente.gerente;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Esqueleto de leitura (Semana 02 do cronograma). Ainda sem HATEOAS/_links
 * (entra na S9) nem nas regras de negócio de R12/R13/R14/R15 (entram na S6+).
 */
@RestController
@RequestMapping("/gerentes")
public class GerenteController {

    private final GerenteService gerenteService;

    public GerenteController(GerenteService gerenteService) {
        this.gerenteService = gerenteService;
    }

    @GetMapping
    public List<Gerente> listarTodos() {
        return gerenteService.listarTodos();
    }

    @GetMapping("/{cpf}")
    public Gerente buscarPorCpf(@PathVariable String cpf) {
        return gerenteService.buscarPorCpf(cpf);
    }
}
