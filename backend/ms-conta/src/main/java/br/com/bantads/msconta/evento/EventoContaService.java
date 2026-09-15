package br.com.bantads.msconta.evento;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lado COMMAND: acrescentar eventos ao stream de uma conta. Usado hoje só
 * internamente para provar o pipeline de CQRS (S4); R4/R5/R6 (S5) e a SAGA
 * R9 (S6) passam a chamar isso para depósito/saque/transferência/criação de
 * conta.
 */
@Service
public class EventoContaService {

    private static final int TENTATIVAS_MAXIMAS = 3;

    private final EventoContaAppender appender;
    private final ObjectMapper objectMapper;

    public EventoContaService(EventoContaAppender appender, ObjectMapper objectMapper) {
        this.appender = appender;
        this.objectMapper = objectMapper;
    }

    public EventoConta registrar(String numeroConta, TipoEvento tipo, Map<String, Object> payload) {
        String payloadJson = escrever(payload);
        DataIntegrityViolationException ultimoConflito = null;

        for (int tentativa = 1; tentativa <= TENTATIVAS_MAXIMAS; tentativa++) {
            try {
                return appender.tentarRegistrar(numeroConta, tipo, payloadJson);
            } catch (DataIntegrityViolationException conflito) {
                // Corrida na mesma conta (ex.: dois saques simultâneos) — outra
                // transação já usou a próxima versão; tenta de novo com a versão
                // seguinte (docs/specs/05-nao-funcionais/10-cqrs.md).
                ultimoConflito = conflito;
            }
        }
        throw ultimoConflito;
    }

    private String escrever(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("payload de evento inválido", e);
        }
    }
}
