package br.com.bantads.msconta.conta;

import br.com.bantads.msconta.evento.EventoConta;
import br.com.bantads.msconta.evento.EventoContaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Lado COMMAND: saldo reconstruído por replay/fold dos eventos, nunca lido do
 * read model. R5/R6 (S5) chamam isto para validar saldo antes de saque/
 * transferência — o read model (ms_conta.contas.saldo) é só para leitura
 * (R3/R7/R11/R16), podendo estar momentaneamente defasado (ver
 * docs/design/modelagem-ct.md e docs/specs/05-nao-funcionais/10-cqrs.md).
 */
@Service
public class SaldoReplayService {

    private final EventoContaRepository eventoContaRepository;
    private final ObjectMapper objectMapper;

    public SaldoReplayService(EventoContaRepository eventoContaRepository, ObjectMapper objectMapper) {
        this.eventoContaRepository = eventoContaRepository;
        this.objectMapper = objectMapper;
    }

    public BigDecimal replaySaldo(String numeroConta) {
        List<EventoConta> eventos = eventoContaRepository.findByObjetoIdOrderByVersaoAsc(numeroConta);

        BigDecimal saldo = BigDecimal.ZERO;
        for (EventoConta evento : eventos) {
            saldo = switch (evento.getTipo()) {
                case CRIADO, GERENTE_ALTERADO -> saldo;
                case DEPOSITO, TRANSFERENCIA_DESTINO -> saldo.add(valorDoPayload(evento));
                case SAQUE, TRANSFERENCIA_ORIGEM -> saldo.subtract(valorDoPayload(evento));
            };
        }
        return saldo;
    }

    private BigDecimal valorDoPayload(EventoConta evento) {
        try {
            JsonNode payload = objectMapper.readTree(evento.getPayload());
            return new BigDecimal(payload.get("valor").asText());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "payload inválido no evento id=" + evento.getId() + " (conta " + evento.getObjetoId() + ")", e);
        }
    }
}
