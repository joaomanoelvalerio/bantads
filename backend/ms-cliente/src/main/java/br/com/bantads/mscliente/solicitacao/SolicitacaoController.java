package br.com.bantads.mscliente.solicitacao;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /clientes — R1 Autocadastro. Mesmo caminho de recurso de ClienteController
 * (GET), mas cria uma Solicitacao (Pendente), não um Cliente diretamente — o
 * cliente só existe após a aprovação do gerente (R9, SAGA — S6). Rota pública
 * (sem sessão) por convenção do front — ver rotas-publicas.ts no frontend.
 */
@RestController
@RequestMapping("/clientes")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    public SolicitacaoController(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    @PostMapping
    public ResponseEntity<Solicitacao> autocadastrar(@Valid @RequestBody NovoClienteRequest requisicao) {
        Solicitacao solicitacao = solicitacaoService.autocadastrar(requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitacao);
    }
}
