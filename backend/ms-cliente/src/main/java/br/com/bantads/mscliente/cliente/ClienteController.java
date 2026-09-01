package br.com.bantads.mscliente.cliente;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Esqueleto de leitura (Semana 02 do cronograma). Ainda sem HATEOAS/_links
 * (entra na S9) nem nas regras de negócio de R1/R11/R16 (entram na S4+).
 */
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<Cliente> listarTodos() {
        return clienteService.listarTodos();
    }

    @GetMapping("/{cpf}")
    public Cliente buscarPorCpf(@PathVariable String cpf) {
        return clienteService.buscarPorCpf(cpf);
    }
}
