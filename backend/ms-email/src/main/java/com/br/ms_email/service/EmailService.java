package com.br.ms_email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void processarEnvioEmail(String tipo, Map<String, Object> payload) {
        log.info("Processando envio de e-mail do tipo: {}", tipo);

        String destinatario = (String) payload.get("email");
        if (destinatario == null || destinatario.isBlank()) {
            log.error("Destinatário de e-mail não informado no payload.");
            return;
        }

        switch (tipo) {
            case "email.enviar-senha":
                String senha = (String) payload.getOrDefault("senha", "Senha123!");
                String nome = (String) payload.getOrDefault("nome", "Cliente");
                enviarEmailSenha(destinatario, nome, senha);
                break;

            case "email.notificar-falha-solicitacao":
                String motivo = (String) payload.getOrDefault("motivo", "Não especificado");
                enviarEmailFalha(destinatario, motivo);
                break;

            default:
                log.warn("Tipo de comando de e-mail desconhecido: {}", tipo);
                break;
        }
    }

    private void enviarEmailSenha(String para, String nome, String senha) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@bantads.com.br");
            message.setTo(para);
            message.setSubject("BANTADS - Conta Aprovada com Sucesso!");
            message.setText("Olá " + nome + ",\n\nSua conta no BANTADS foi aprovada!\n" +
                           "Sua senha de acesso inicial é: " + senha + "\n\n" +
                           "Recomendamos alterá-la no seu primeiro acesso.\n\nAtenciosamente,\nEquipe BANTADS.");

            mailSender.send(message);
            log.info("E-mail com senha enviado com sucesso para: {}", para);
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de senha para {}", para, e);
        }
    }

    private void enviarEmailFalha(String para, String motivo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@bantads.com.br");
            message.setTo(para);
            message.setSubject("BANTADS - Atualização da Solicitação de Cadastro");
            message.setText("Olá,\n\nInfelizmente não foi possível concluir a abertura da sua conta no BANTADS.\n" +
                           "Motivo: " + motivo + "\n\nPor favor, entre em contato com o suporte ou tente novamente.\n\n" +
                           "Atenciosamente,\nEquipe BANTADS.");

            mailSender.send(message);
            log.info("E-mail de notificação de falha enviado com sucesso para: {}", para);
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de falha para {}", para, e);
        }
    }
}