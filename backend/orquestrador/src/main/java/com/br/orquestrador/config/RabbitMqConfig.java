package com.br.orquestrador.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson2JsonMessageConverter é a classe legada (Jackson 2.x); Spring Boot 4
 * usa Jackson 3 (pacote tools.jackson.*, não com.fasterxml.jackson.*), e
 * aquela classe não resolve mais em runtime (NoClassDefFoundError) — mesmo
 * ajuste já feito no MS Email. JacksonJsonMessageConverter é a equivalente
 * para Jackson 3.
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter conversor) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(conversor);
        return template;
    }
}
