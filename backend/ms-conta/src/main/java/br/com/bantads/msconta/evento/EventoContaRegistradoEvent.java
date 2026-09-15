package br.com.bantads.msconta.evento;

/**
 * Evento interno do Spring (não é o evento de domínio) — dispara a publicação
 * em ms.conta.events só depois da transação confirmar (ver EventoContaPublicador),
 * para nunca publicar um evento cuja escrita acabou sendo revertida.
 */
public record EventoContaRegistradoEvent(Long eventoId) {
}
