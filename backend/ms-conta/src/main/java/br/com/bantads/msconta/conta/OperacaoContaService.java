package br.com.bantads.msconta.conta;

import br.com.bantads.msconta.evento.EventoContaAppender;
import br.com.bantads.msconta.evento.EventoContaService;
import br.com.bantads.msconta.evento.TipoEvento;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * R4 (depósito), R5 (saque) e R6 (transferência) — docs/specs/02-requisitos-funcionais.md.
 * Depósito só acrescenta um evento (EventoContaService cuida do retry de
 * versão). Saque e transferência debitam de uma conta, então cada tentativa
 * precisa refazer o replay do saldo — não basta re-tentar o append: se a
 * corrida for justamente por causa do saldo (duas operações concorrentes que,
 * juntas, estourariam o saldo), só a releitura pós-conflito pega isso
 * (docs/specs/05-nao-funcionais/10-cqrs.md, "a operação perdedora refaz o
 * replay e revalida").
 */
@Service
public class OperacaoContaService {

    private static final int TENTATIVAS_MAXIMAS = 5;

    private final ContaService contaService;
    private final ContaRepository contaRepository;
    private final SaldoReplayService saldoReplayService;
    private final EventoContaService eventoContaService;
    private final EventoContaAppender eventoContaAppender;
    private final ObjectMapper objectMapper;

    public OperacaoContaService(
            ContaService contaService,
            ContaRepository contaRepository,
            SaldoReplayService saldoReplayService,
            EventoContaService eventoContaService,
            EventoContaAppender eventoContaAppender,
            ObjectMapper objectMapper) {
        this.contaService = contaService;
        this.contaRepository = contaRepository;
        this.saldoReplayService = saldoReplayService;
        this.eventoContaService = eventoContaService;
        this.eventoContaAppender = eventoContaAppender;
        this.objectMapper = objectMapper;
    }

    /** R4 — não valida saldo (depósito sempre pode acontecer). */
    public void depositar(String numeroConta, String cpfSolicitante, BigDecimal valor) {
        contaService.buscarEVerificarPosse(numeroConta, cpfSolicitante);
        eventoContaService.registrar(numeroConta, TipoEvento.DEPOSITO, Map.of("valor", valor.toPlainString()));
    }

    /** R5 — só há saque se houver saldo suficiente; saldo vem sempre do replay do command. */
    public void sacar(String numeroConta, String cpfSolicitante, BigDecimal valor) {
        contaService.buscarEVerificarPosse(numeroConta, cpfSolicitante);
        String payload = escrever(Map.of("valor", valor.toPlainString()));

        DataIntegrityViolationException ultimoConflito = null;
        for (int tentativa = 1; tentativa <= TENTATIVAS_MAXIMAS; tentativa++) {
            exigirSaldoSuficiente(numeroConta, valor);
            try {
                eventoContaAppender.tentarRegistrar(numeroConta, TipoEvento.SAQUE, payload);
                return;
            } catch (DataIntegrityViolationException conflito) {
                ultimoConflito = conflito;
            }
        }
        throw ultimoConflito;
    }

    /**
     * R6 — dois eventos atômicos (não é SAGA). Conta destino precisa existir;
     * saldo validado na conta de origem, mesma lógica de retry do saque.
     */
    public void transferir(String numeroContaOrigem, String cpfSolicitante, TransferenciaRequest requisicao) {
        Conta origem = contaService.buscarEVerificarPosse(numeroContaOrigem, cpfSolicitante);

        if (numeroContaOrigem.equals(requisicao.getContaDestino())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível transferir para a própria conta");
        }
        Conta destino = contaRepository.findById(requisicao.getContaDestino())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta destino não encontrada"));

        BigDecimal valor = requisicao.getValor();
        Map<String, Object> camposOrigem = new LinkedHashMap<>();
        camposOrigem.put("valor", valor.toPlainString());
        camposOrigem.put("contaDestino", destino.getNumeroConta());
        camposOrigem.put("cpfDestino", destino.getCpfCliente());
        camposOrigem.put("nomeDestino", vazioParaNull(requisicao.getNomeDestino())); // Map.of não aceita null; LinkedHashMap sim
        String payloadOrigem = escrever(camposOrigem);

        Map<String, Object> camposDestino = new LinkedHashMap<>();
        camposDestino.put("valor", valor.toPlainString());
        camposDestino.put("contaOrigem", origem.getNumeroConta());
        camposDestino.put("cpfOrigem", origem.getCpfCliente());
        camposDestino.put("nomeOrigem", vazioParaNull(requisicao.getNomeOrigem()));
        String payloadDestino = escrever(camposDestino);

        DataIntegrityViolationException ultimoConflito = null;
        for (int tentativa = 1; tentativa <= TENTATIVAS_MAXIMAS; tentativa++) {
            exigirSaldoSuficiente(numeroContaOrigem, valor);
            try {
                eventoContaAppender.tentarRegistrarTransferencia(
                        numeroContaOrigem, payloadOrigem, destino.getNumeroConta(), payloadDestino);
                return;
            } catch (DataIntegrityViolationException conflito) {
                ultimoConflito = conflito;
            }
        }
        throw ultimoConflito;
    }

    private void exigirSaldoSuficiente(String numeroConta, BigDecimal valor) {
        BigDecimal saldoAtual = saldoReplayService.replaySaldo(numeroConta);
        if (saldoAtual.compareTo(valor) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente");
        }
    }

    /** Nome não informado (Gateway ainda não enriquece a requisição) vira `null` no payload, não "". */
    private String vazioParaNull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto;
    }

    private String escrever(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("payload de evento inválido", e);
        }
    }
}
