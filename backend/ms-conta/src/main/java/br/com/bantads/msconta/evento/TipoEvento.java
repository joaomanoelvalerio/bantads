package br.com.bantads.msconta.evento;

/** Espelha o CHECK constraint de ms_conta.eventos_conta.tipo. */
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

    public String getValorNoBanco() {
        return valorNoBanco;
    }

    public static TipoEvento deValorNoBanco(String valor) {
        for (TipoEvento tipo : values()) {
            if (tipo.valorNoBanco.equals(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de evento desconhecido: " + valor);
    }
}
