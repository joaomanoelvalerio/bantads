package com.br.ms_email.listener;

import com.br.ms_email.config.RabbitMQConfig;
import com.br.ms_email.dto.EmailMessage;
import com.br.ms_email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL_CMD)
    public void receberComandoEmail(EmailMessage mensagem) {
        log.info("Comando de e-mail recebido: sagaId={} tipo={}", mensagem.getSagaId(), mensagem.getTipo());
        try {
            emailService.processarEnvioEmail(mensagem.getTipo(), mensagem.getPayload());
        } catch (Exception e) {
            log.error("Erro ao processar mensagem de e-mail da SAGA {}", mensagem.getSagaId(), e);
        }
    }
}