package br.com.bantads.msconta.conta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Esqueleto de leitura do read model (Semana 02 do cronograma). Depósito/saque/
 * transferência/extrato (R3-R7) e HATEOAS (_links, S9) entram em semanas
 * seguintes — ver docs/design/modelagem-ct.md.
 */
@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService contaService;

    public ContaController(ContaService contaService) {
        this.contaService = contaService;
    }

    @GetMapping("/{numeroConta}")
    public Conta buscarPorNumero(@PathVariable String numeroConta) {
        return contaService.buscarPorNumero(numeroConta);
    }
}
