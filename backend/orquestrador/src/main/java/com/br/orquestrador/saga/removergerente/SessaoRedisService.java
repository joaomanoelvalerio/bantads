package com.br.orquestrador.saga.removergerente;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * SAGA Remover Gerente (R15, passo 3) — "apaga no Redis a sessão do gerente
 * removido (DEL em sessao:cpf:&lt;cpf&gt; e sessao:&lt;jti&gt;), logout forçado".
 * Sem
 * compensação (a própria spec diz: "se a SAGA falhar, o gerente reativado
 * faz novo login") e sem RabbitMQ — é a mesma instância de Redis que o
 * Gateway usa pra sessão (`backend/api-gateway/sessao.js`), acessada direto
 * via `StringRedisTemplate`, sem round-trip de comando/resposta.
 */
@Component
public class SessaoRedisService {

    private static final Logger log = LoggerFactory.getLogger(SessaoRedisService.class);

    private final StringRedisTemplate redis;

    public SessaoRedisService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void encerrarSessaoDoGerente(String cpf) {
        String chaveReversa = "sessao:cpf:" + cpf;
        String jti = redis.opsForValue().get(chaveReversa);

        if (jti == null) {
            log.info("Gerente {} não tinha sessão ativa no Redis — nada a apagar", cpf);
            return;
        }

        redis.delete("sessao:" + jti);
        redis.delete(chaveReversa);
        log.info("Sessão do gerente {} apagada do Redis (logout forçado)", cpf);
    }
}
