package br.com.bantads.msconta.conta;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * R3-R7 (docs/specs/02-requisitos-funcionais.md). `X-User-CPF` é injetado
 * pelo Gateway após validar a sessão (docs/specs/05-nao-funcionais/03-api-gateway.md)
 * — aqui só confiamos nele; sem Gateway na frente, é preciso enviá-lo manualmente.
 * HATEOAS (`_links`) entra na S9 — ver docs/design/modelagem-ct.md.
 */
@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService contaService;
    private final OperacaoContaService operacaoContaService;
    private final ExtratoService extratoService;

    public ContaController(ContaService contaService, OperacaoContaService operacaoContaService, ExtratoService extratoService) {
        this.contaService = contaService;
        this.operacaoContaService = operacaoContaService;
        this.extratoService = extratoService;
    }

    /**
     * Uso interno — o Gateway compõe R11/R12/R16 com isto (saldo por
     * cliente, contagem por gerente). Não é pra ficar acessível direto por
     * um cliente comum (devolve saldo de todo mundo); o Gateway bloqueia o
     * path raiz de `/contas` no proxy genérico e só chama isto
     * servidor-a-servidor — ver `backend/api-gateway/index.js`.
     */
    @GetMapping
    public List<Conta> listarTodas() {
        return contaService.listarTodas();
    }

    @GetMapping("/{numeroConta}")
    public Conta buscarPorNumero(@PathVariable String numeroConta) {
        return contaService.buscarPorNumero(numeroConta);
    }

    /** R3: o front não sabe o próprio número de conta de antemão — resolve pelo CPF da sessão. */
    @GetMapping("/cliente/{cpf}")
    public Conta buscarPorCpfCliente(@PathVariable String cpf) {
        return contaService.buscarPorCpfCliente(cpf);
    }

    /** R4 — não devolve o novo saldo; front reconsulta GET /contas/{numeroConta} depois. */
    @PostMapping("/{numeroConta}/deposito")
    public ResponseEntity<Void> depositar(
            @PathVariable String numeroConta,
            @RequestHeader("X-User-CPF") String cpfSolicitante,
            @Valid @RequestBody DepositoRequest requisicao) {
        operacaoContaService.depositar(numeroConta, cpfSolicitante, requisicao.getValor());
        return ResponseEntity.ok().build();
    }

    /** R5 — 422 se saldo insuficiente. */
    @PostMapping("/{numeroConta}/saque")
    public ResponseEntity<Void> sacar(
            @PathVariable String numeroConta,
            @RequestHeader("X-User-CPF") String cpfSolicitante,
            @Valid @RequestBody SaqueRequest requisicao) {
        operacaoContaService.sacar(numeroConta, cpfSolicitante, requisicao.getValor());
        return ResponseEntity.ok().build();
    }

    /** R6 — 404 se conta destino não existe, 422 se saldo insuficiente. */
    @PostMapping("/{numeroConta}/transferencia")
    public ResponseEntity<Void> transferir(
            @PathVariable String numeroConta,
            @RequestHeader("X-User-CPF") String cpfSolicitante,
            @Valid @RequestBody TransferenciaRequest requisicao) {
        operacaoContaService.transferir(numeroConta, cpfSolicitante, requisicao);
        return ResponseEntity.ok().build();
    }

    /** R7 — padrão: últimos 30 dias; máximo 365 dias de intervalo. */
    @GetMapping("/{numeroConta}/extrato")
    public ExtratoResponse extrato(
            @PathVariable String numeroConta,
            @RequestHeader("X-User-CPF") String cpfSolicitante,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return extratoService.consultar(numeroConta, cpfSolicitante, dataInicio, dataFim);
    }
}
