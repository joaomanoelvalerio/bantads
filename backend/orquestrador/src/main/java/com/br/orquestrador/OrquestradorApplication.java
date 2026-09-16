package com.br.orquestrador;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OrquestradorApplication {

  public static final String QUEUE_SAGA_CMD = "saga.cmd";
  public static final String QUEUE_ORQUESTRADOR_REPLY = "orquestrador.reply";

  public static final String QUEUE_MS_CLIENTE_CMD = "ms.cliente.cmd";
  public static final String QUEUE_MS_CONTA_CMD = "ms.conta.cmd";
  public static final String QUEUE_MS_GERENTE_CMD = "ms.gerente.cmd";
  public static final String QUEUE_MS_AUTH_CMD = "ms.auth.cmd";
  public static final String QUEUE_MS_EMAIL_CMD = "ms.email.cmd";

  public static final String QUEUE_MS_CLIENTE_DLQ = "ms.cliente.cmd.dlq";
  public static final String QUEUE_MS_CONTA_DLQ = "ms.conta.cmd.dlq";
  public static final String QUEUE_MS_GERENTE_DLQ = "ms.gerente.cmd.dlq";
  public static final String QUEUE_MS_AUTH_DLQ = "ms.auth.cmd.dlq";

  public static void main(String[] args) {
    SpringApplication.run(OrquestradorApplication.class, args);
  }

  @Bean
  public Queue sagaCmdQueue() {
    return QueueBuilder.durable(QUEUE_SAGA_CMD).build();
  }

  @Bean
  public Queue orquestradorReplyQueue() {
    return QueueBuilder.durable(QUEUE_ORQUESTRADOR_REPLY).build();
  }

  @Bean
  public Queue msClienteCmdQueue() {
    return QueueBuilder.durable(QUEUE_MS_CLIENTE_CMD)
        .deadLetterExchange("")
        .deadLetterRoutingKey(QUEUE_MS_CLIENTE_DLQ)
        .build();
  }

  @Bean
  public Queue msClienteDlqQueue() {
    return QueueBuilder.durable(QUEUE_MS_CLIENTE_DLQ).build();
  }

  @Bean
  public Queue msContaCmdQueue() {
    return QueueBuilder.durable(QUEUE_MS_CONTA_CMD)
        .deadLetterExchange("")
        .deadLetterRoutingKey(QUEUE_MS_CONTA_DLQ)
        .build();
  }

  @Bean
  public Queue msContaDlqQueue() {
    return QueueBuilder.durable(QUEUE_MS_CONTA_DLQ).build();
  }

  @Bean
  public Queue msGerenteCmdQueue() {
    return QueueBuilder.durable(QUEUE_MS_GERENTE_CMD)
        .deadLetterExchange("")
        .deadLetterRoutingKey(QUEUE_MS_GERENTE_DLQ)
        .build();
  }

  @Bean
  public Queue msGerenteDlqQueue() {
    return QueueBuilder.durable(QUEUE_MS_GERENTE_DLQ).build();
  }

  @Bean
  public Queue msAuthCmdQueue() {
    return QueueBuilder.durable(QUEUE_MS_AUTH_CMD)
        .deadLetterExchange("")
        .deadLetterRoutingKey(QUEUE_MS_AUTH_DLQ)
        .build();
  }

  @Bean
  public Queue msAuthDlqQueue() {
    return QueueBuilder.durable(QUEUE_MS_AUTH_DLQ).build();
  }

  @Bean
  public Queue msEmailCmdQueue() {
    return QueueBuilder.durable(QUEUE_MS_EMAIL_CMD).build();
  }
}
