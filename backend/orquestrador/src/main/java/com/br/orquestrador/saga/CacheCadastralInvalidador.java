package com.br.orquestrador.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CacheCadastralInvalidador {

    private static final Logger log = LoggerFactory.getLogger(CacheCadastralInvalidador.class);

    private final StringRedisTemplate redis;

    public CacheCadastralInvalidador(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void invalidarCliente(String cpf) {
        invalidar("cache:cliente:" + cpf);
    }

    public void invalidarGerente(String cpf) {
        invalidar("cache:gerente:" + cpf);
    }

    private void invalidar(String chave) {
        try {
            redis.delete(chave);
        } catch (Exception e) {
            log.warn("Falha ao invalidar {} no Redis — expira sozinha pelo TTL", chave, e);
        }
    }
}
