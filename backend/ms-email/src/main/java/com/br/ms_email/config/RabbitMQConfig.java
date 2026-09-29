package com.br.ms_email.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_EMAIL_CMD = "ms.email.cmd";

    @Bean
    public Queue emailCmdQueue() {
        return new Queue(QUEUE_EMAIL_CMD, true);
    }

    // Jackson2JsonMessageConverter é a classe legada (Jackson 2.x); Spring Boot 4
    // usa Jackson 3 (pacote tools.jackson.*, não com.fasterxml.jackson.*), e
    // aquela classe não resolve mais em runtime (NoClassDefFoundError).
    // JacksonJsonMessageConverter é a equivalente para Jackson 3.
    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}