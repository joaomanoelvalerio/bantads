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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
    private final RabbitTemplate rabbitTemplate;

    public SolicitacaoConsultaController(SolicitacaoService solicitacaoService, RabbitTemplate rabbitTemplate) {
        this.solicitacaoService = solicitacaoService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping
    public List<Solicitacao> listar() {
        return solicitacaoService.listarTodas();
    }

    /**
     * R10 — síncrono (200), diferente de R9: não é SAGA, não passa pelo
     * Orquestrador. O e-mail é publicado direto daqui, fire-and-forget, sem
     * `sagaId` (docs/specs/05-nao-funcionais/07-rabbitmq-filas.md —
     * "mensagens fora de SAGA: não definir sagaId").
     */
    @PostMapping("/{cpf}/rejeitar")
    public void rejeitar(@PathVariable String cpf, @Valid @RequestBody RejeicaoRequest requisicao) {
        Optional<Solicitacao> rejeitada = solicitacaoService.rejeitarPendente(cpf, requisicao.getMotivo());
        Solicitacao solicitacao = rejeitada.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação Pendente não encontrada para o CPF " + cpf));

        ComandoSaga mensagem = new ComandoSaga(
                null, "email.notificar-falha-solicitacao", OffsetDateTime.now().toString(),
                Map.of("email", solicitacao.getEmail(), "motivo", requisicao.getMotivo()));
        rabbitTemplate.convertAndSend(RabbitMqConfig.FILA_EMAIL, mensagem);
    }

    @Getter
    @Setter
    public static class RejeicaoRequest {
        @NotBlank
        private String motivo;
    }
}
