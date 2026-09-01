package br.com.bantads.msconta.conta;

/** Espelha o CHECK constraint de ms_conta.movimentacoes.tipo — grafia de R7 (minúscula). */
public enum TipoMovimentacao {
    DEPOSITO("depósito"),
    SAQUE("saque"),
    TRANSFERENCIA("transferência");

    private final String valorNoBanco;

    TipoMovimentacao(String valorNoBanco) {
        this.valorNoBanco = valorNoBanco;
    }

    public String getValorNoBanco() {
        return valorNoBanco;
    }

    public static TipoMovimentacao deValorNoBanco(String valor) {
        for (TipoMovimentacao tipo : values()) {
            if (tipo.valorNoBanco.equals(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de movimentação desconhecido: " + valor);
    }
}
