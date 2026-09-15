package br.com.bantads.msconta.evento;

import java.time.OffsetDateTime;

/**
 * Payload publicado em ms.conta.events — o consumidor (read model) aplica isso
 * de forma idempotente (ver br.com.bantads.msconta.conta.ProjecaoContaListener).
 */
public record EventoContaMensagem(
        Long id,
        String objetoId,
        TipoEvento tipo,
        String payload,
        Integer versao,
        OffsetDateTime timestamp) {

    public static EventoContaMensagem de(EventoConta evento) {
        return new EventoContaMensagem(
                evento.getId(),
                evento.getObjetoId(),
                evento.getTipo(),
                evento.getPayload(),
                evento.getVersao(),
                evento.getTimestamp());
    }
}
