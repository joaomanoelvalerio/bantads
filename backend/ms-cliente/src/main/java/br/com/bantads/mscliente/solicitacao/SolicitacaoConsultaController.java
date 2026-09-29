package br.com.bantads.mscliente.solicitacao;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * R8 — tela inicial do gerente: solicitações em todos os estados. Path
 * separado de `/clientes` (onde mora o POST de autocadastro, R1) porque o
 * recurso é outro (Solicitação, não Cliente) e o contrato do front já espera
 * `GET /solicitacoes` (`SolicitacaoService.listar()` no frontend).
 */
@RestController
@RequestMapping("/solicitacoes")
public class SolicitacaoConsultaController {

    private final SolicitacaoService solicitacaoService;

    public SolicitacaoConsultaController(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    @GetMapping
    public List<Solicitacao> listar() {
        return solicitacaoService.listarTodas();
    }
}
