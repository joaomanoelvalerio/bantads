package br.com.bantads.mscliente.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SAGA Aprovar Cliente (R9, S6) — redeclara `ms.cliente.cmd`/`orquestrador.reply`
 * com os mesmos argumentos que o Orquestrador (dono das filas) já declara,
 * para este serviço não depender da ordem de subida (docs/specs/05-nao-funcionais/07-rabbitmq-filas.md).
 */
@Configuration
public class RabbitMqConfig {

    public static final String FILA_COMANDO = "ms.cliente.cmd";
    public static final String FILA_COMANDO_DLQ = "ms.cliente.cmd.dlq";
    public static final String FILA_RESPOSTA = "orquestrador.reply";

    @Bean
    public Queue filaComando() {
        return QueueBuilder.durable(FILA_COMANDO)
                .deadLetterExchange("")
                .deadLetterRoutingKey(FILA_COMANDO_DLQ)
                .build();
    }

    @Bean
    public Queue filaResposta() {
        return QueueBuilder.durable(FILA_RESPOSTA).build();
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
