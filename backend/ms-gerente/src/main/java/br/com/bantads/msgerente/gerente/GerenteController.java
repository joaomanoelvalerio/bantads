package br.com.bantads.msgerente.gerente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * R13 (inserção) não tem endpoint
 * REST aqui — é SAGA, entra só pelo Orquestrador via `ms.gerente.cmd`
 * (ver `saga/GerenteComandoListener`).
 */
@RestController
@RequestMapping("/gerentes")
public class GerenteController {

    private final GerenteService gerenteService;

    public GerenteController(GerenteService gerenteService) {
        this.gerenteService = gerenteService;
    }

    /** R12 — só gerentes ativos; a quantidade de clientes é composta pelo Gateway (dado do MS Conta). */
    @GetMapping
    public List<Gerente> listarAtivos() {
        return gerenteService.listarAtivos();
    }

    @GetMapping("/{cpf}")
    public Gerente buscarPorCpf(@PathVariable String cpf) {
        return gerenteService.buscarPorCpf(cpf);
    }

    /** R14 — síncrono; CPF/e-mail não vêm no corpo porque não são alteráveis. */
    @PutMapping("/{cpf}")
    public Gerente atualizar(@PathVariable String cpf, @Valid @RequestBody AtualizacaoGerenteRequest requisicao) {
        return gerenteService.atualizar(cpf, requisicao.getNome(), requisicao.getTelefone());
    }

    @Getter
    @Setter
    public static class AtualizacaoGerenteRequest {
        @NotBlank
        private String nome;

        @NotBlank
        private String telefone;
    }
}
