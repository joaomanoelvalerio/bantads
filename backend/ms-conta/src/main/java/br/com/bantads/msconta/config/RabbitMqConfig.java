package br.com.bantads.msconta.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sincronização do CQRS (comando -&gt; query) via `ms.conta.events`, default
 * exchange (docs/specs/05-nao-funcionais/07-rabbitmq-filas.md). DLQ dedicada
 * — mensagens que esgotam as retentativas do listener (ver application.yml)
 * são roteadas para cá; reprocessamento é manual (não automático, para evitar
 * loop com mensagem "venenosa" — ver docs/specs/05-nao-funcionais/07-rabbitmq-filas.md,
 * "Caso especial — ms.conta.events.dlq").
 */
@Configuration
public class RabbitMqConfig {

    public static final String FILA_EVENTOS = "ms.conta.events";
    public static final String FILA_EVENTOS_DLQ = "ms.conta.events.dlq";

    @Bean
    public Queue filaEventosDlq() {
        return QueueBuilder.durable(FILA_EVENTOS_DLQ).build();
    }

    @Bean
    public Queue filaEventos() {
        return QueueBuilder.durable(FILA_EVENTOS)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", FILA_EVENTOS_DLQ)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter conversor) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(conversor);
        return template;
    }
}
