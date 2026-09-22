package br.com.bantads.msconta.evento;

/** Os dois eventos de uma transferência (R6), sempre gravados juntos. */
public record ParTransferencia(EventoConta origem, EventoConta destino) {
}
