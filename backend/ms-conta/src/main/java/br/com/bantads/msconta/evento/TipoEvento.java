package br.com.bantads.msconta.evento;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Espelha o CHECK constraint de ms_conta.eventos_conta.tipo. @JsonValue/
 * @JsonCreator fazem o JSON (mensagem AMQP em ms.conta.events, e futuros
 * endpoints) trafegar com o mesmo literal grafado na especificação
 * ("Depósito", não "DEPOSITO") — sem isso o Jackson usaria o nome Java do
 * enum por padrão.
 */
public enum TipoEvento {
    CRIADO("Criado"),
    SAQUE("Saque"),
    DEPOSITO("Depósito"),
    TRANSFERENCIA_ORIGEM("TransferênciaOrigem"),
    TRANSFERENCIA_DESTINO("TransferênciaDestino"),
    GERENTE_ALTERADO("GerenteAlterado");

    private final String valorNoBanco;

    TipoEvento(String valorNoBanco) {
        this.valorNoBanco = valorNoBanco;
    }

    @JsonValue
    public String getValorNoBanco() {
        return valorNoBanco;
    }

    @JsonCreator
    public static TipoEvento deValorNoBanco(String valor) {
        for (TipoEvento tipo : values()) {
            if (tipo.valorNoBanco.equals(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de evento desconhecido: " + valor);
    }
}
