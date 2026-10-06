package com.br.orquestrador.saga;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Estado da SAGA no Redis, chave
 * `saga:&lt;sagaId&gt;`, TTL 1h. Só para acompanhamento/depuração (ex.: console
 * do RabbitMQ + Redis na defesa) — quem decide o fluxo é o código Java, não
 * uma releitura deste estado. O `payload` gravado aqui nunca inclui dados
 * sensíveis (a senha do passo 5 nunca passa por aqui).
 */
@Component
public class SagaEstadoRepositorio {

    private static final Logger log = LoggerFactory.getLogger(SagaEstadoRepositorio.class);
    private static final Duration TTL = Duration.ofHours(1);
    private static final String PREFIXO = "saga:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SagaEstadoRepositorio(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void salvar(String sagaId, String tipo, int etapaAtual, String status, Map<String, Object> payload) {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("sagaId", sagaId);
        estado.put("tipo", tipo);
        estado.put("etapaAtual", etapaAtual);
        estado.put("status", status);
        estado.put("payload", payload);
        estado.put("timestamp", OffsetDateTime.now().toString());

        try {
            redis.opsForValue().set(PREFIXO + sagaId, objectMapper.writeValueAsString(estado), TTL);
        } catch (Exception e) {
            log.error("Falha ao gravar o estado da saga {} no Redis", sagaId, e);
        }
    }
}
