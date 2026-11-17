package br.com.bantads.mscliente.solicitacao;

import br.com.bantads.mscliente.config.RabbitMqConfig;
import br.com.bantads.mscliente.saga.ComandoSaga;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tudo sob /solicitacoes: R1 (autocadastro, público — ver rotas-publicas.ts
 * no frontend), R8 (listagem/consulta, GERENTE) e R10 (rejeição, GERENTE). R9
 * (aprovação) é SAGA, entra só pelo Orquestrador via `ms.cliente.cmd` — ver
 * `saga/ClienteComandoListener`.
 */
@RestController
@RequestMapping("/solicitacoes")
public class SolicitacaoConsultaController {

    private final SolicitacaoService solicitacaoService;
    private final RabbitTemplate rabbitTemplate;

    public SolicitacaoConsultaController(SolicitacaoService solicitacaoService, RabbitTemplate rabbitTemplate) {
        this.solicitacaoService = solicitacaoService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /** R1 — Autocadastro. Público (sem sessão) por convenção do front. */
    @PostMapping
    public ResponseEntity<Solicitacao> autocadastrar(@Valid @RequestBody NovoClienteRequest requisicao) {
        Solicitacao solicitacao = solicitacaoService.autocadastrar(requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitacao);
    }

    /** R8 — todas as solicitações, em qualquer status. */
    @GetMapping
    public Map<String, List<Solicitacao>> listar() {
        return Map.of("solicitacoes", solicitacaoService.listarTodas());
    }

    /** R8 — consulta uma solicitação específica. */
    @GetMapping("/{cpf}")
    public Solicitacao obter(@PathVariable String cpf) {
        return solicitacaoService.buscarPorCpf(cpf)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada para o CPF " + cpf));
    }

    /**
     * R10 — síncrono (200, devolve a solicitação atualizada), diferente de
     * R9: não é SAGA, não passa pelo Orquestrador. O e-mail é publicado
     * direto daqui, fire-and-forget, sem `sagaId` — "mensagens fora de SAGA:
     * não definir sagaId".
     */
    @PostMapping("/{cpf}/rejeicao")
    public Solicitacao rejeitar(@PathVariable String cpf, @Valid @RequestBody RejeicaoRequest requisicao) {
        Optional<Solicitacao> rejeitada = solicitacaoService.rejeitarPendente(cpf, requisicao.getMotivo());
        Solicitacao solicitacao = rejeitada.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação Pendente não encontrada para o CPF " + cpf));

        ComandoSaga mensagem = new ComandoSaga(
                null, "email.notificar-falha-solicitacao", OffsetDateTime.now().toString(),
                Map.of("email", solicitacao.getEmail(), "motivo", requisicao.getMotivo()));
        rabbitTemplate.convertAndSend(RabbitMqConfig.FILA_EMAIL, mensagem);

        return solicitacao;
    }

    @Getter
    @Setter
    public static class RejeicaoRequest {
        @NotBlank
        private String motivo;
    }
}
