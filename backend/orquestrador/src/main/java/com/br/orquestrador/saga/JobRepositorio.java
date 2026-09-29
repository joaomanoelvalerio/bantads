package com.br.orquestrador.saga;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Job assíncrono no Redis (docs/specs/05-nao-funcionais/08-jobs-assincronos.md).
 * Chave `job:&lt;jobId&gt;`, TTL 5 min (docs/specs/05-nao-funcionais/05-redis.md)
 * — o Gateway cria o job como PENDENTE ao publicar em `saga.cmd`; aqui só se
 * grava o desfecho (CONCLUIDO/FALHA), reescrevendo a mesma chave por
 * completo. As duas pontas (Gateway em Node, aqui em Java) precisam
 * concordar exatamente no prefixo da chave e nos nomes dos campos — são o
 * mesmo contrato JSON, só lido/escrito de dois runtimes diferentes.
 */
@Component
public class JobRepositorio {

    private static final Logger log = LoggerFactory.getLogger(JobRepositorio.class);
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String PREFIXO = "job:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public JobRepositorio(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void marcarConcluidoComoRecurso(String jobId, String dominio, String resourceId) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("jobId", jobId);
        job.put("status", "CONCLUIDO");
        job.put("resultType", "resource");
        job.put("dominio", dominio);
        job.put("resourceId", resourceId);
        job.put("erro", null);
        salvar(jobId, job);
    }

    public void marcarFalha(String jobId, String erro) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("jobId", jobId);
        job.put("status", "FALHA");
        job.put("resultType", null);
        job.put("dominio", null);
        job.put("resourceId", null);
        job.put("erro", erro);
        salvar(jobId, job);
    }

    private void salvar(String jobId, Map<String, Object> job) {
        try {
            redis.opsForValue().set(PREFIXO + jobId, objectMapper.writeValueAsString(job), TTL);
        } catch (Exception e) {
            log.error("Falha ao gravar o desfecho do job {} no Redis", jobId, e);
        }
    }
}
