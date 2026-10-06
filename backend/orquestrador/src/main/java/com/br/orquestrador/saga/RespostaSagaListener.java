package com.br.orquestrador.saga;

import com.br.orquestrador.OrquestradorApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de `orquestrador.reply` — fila única compartilhada por todos os
 * MSs. Só repassa a
 * resposta pro publicador casar com quem está esperando; nunca loga
 * `payload` aqui (é onde a senha em claro do passo 5 da SAGA Aprovar Cliente
 * trafega).
 */
@Component
public class RespostaSagaListener {

    private static final Logger log = LoggerFactory.getLogger(RespostaSagaListener.class);

    private final ComandoSagaPublicador publicador;

    public RespostaSagaListener(ComandoSagaPublicador publicador) {
        this.publicador = publicador;
    }

    @RabbitListener(queues = OrquestradorApplication.QUEUE_ORQUESTRADOR_REPLY)
    public void aoReceberResposta(RespostaSaga resposta) {
        log.info(
                "Resposta SAGA recebida em orquestrador.reply: sagaId={} tipo={} status={}",
                resposta.getSagaId(), resposta.getTipo(), resposta.getStatus());
        publicador.completar(resposta);
    }
}
