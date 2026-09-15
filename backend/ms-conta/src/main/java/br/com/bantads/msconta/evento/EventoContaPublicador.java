package br.com.bantads.msconta.evento;

import br.com.bantads.msconta.config.RabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Só publica em ms.conta.events depois que a transação que gravou o evento
 * confirmar (AFTER_COMMIT) — nunca antes, senão uma escrita revertida
 * publicaria um evento "fantasma" para a projeção.
 */
@Component
public class EventoContaPublicador {

    private final EventoContaRepository eventoContaRepository;
    private final RabbitTemplate rabbitTemplate;

    public EventoContaPublicador(EventoContaRepository eventoContaRepository, RabbitTemplate rabbitTemplate) {
        this.eventoContaRepository = eventoContaRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmarEvento(EventoContaRegistradoEvent evento) {
        EventoConta eventoConta = eventoContaRepository.findById(evento.eventoId())
                .orElseThrow(() -> new IllegalStateException("Evento confirmado não encontrado: " + evento.eventoId()));
        rabbitTemplate.convertAndSend(RabbitMqConfig.FILA_EVENTOS, EventoContaMensagem.de(eventoConta));
    }
}
