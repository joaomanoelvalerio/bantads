package br.com.bantads.msconta.saga;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SAGA Aprovar Cliente (R9, S6) — filas de comando/resposta, separadas de
 * `br.com.bantads.msconta.config.RabbitMqConfig` (que já declara
 * `ms.conta.events` e o `RabbitTemplate`/conversor JSON reaproveitados aqui).
 * Redeclara `ms.conta.cmd`/`orquestrador.reply` com os mesmos argumentos que
 * o Orquestrador (dono das filas) já declara, para este serviço não depender
 * da ordem de subida.
 */
@Configuration
public class RabbitMqConfigSaga {

    public static final String FILA_COMANDO = "ms.conta.cmd";
    public static final String FILA_COMANDO_DLQ = "ms.conta.cmd.dlq";
    public static final String FILA_RESPOSTA = "orquestrador.reply";

    @Bean
    public Queue filaComandoSaga() {
        return QueueBuilder.durable(FILA_COMANDO)
                .deadLetterExchange("")
                .deadLetterRoutingKey(FILA_COMANDO_DLQ)
                .build();
    }

    @Bean
    public Queue filaRespostaSaga() {
        return QueueBuilder.durable(FILA_RESPOSTA).build();
    }
}
