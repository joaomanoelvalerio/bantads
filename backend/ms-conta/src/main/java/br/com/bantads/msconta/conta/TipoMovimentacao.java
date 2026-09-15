package br.com.bantads.msconta.conta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Espelha o CHECK constraint de ms_conta.movimentacoes.tipo — grafia de R7
 * (minúscula). @JsonValue/@JsonCreator fazem o JSON usar esse literal, não o
 * nome do enum Java.
 */
public enum TipoMovimentacao {
    DEPOSITO("depósito"),
    SAQUE("saque"),
    TRANSFERENCIA("transferência");

    private final String valorNoBanco;

    TipoMovimentacao(String valorNoBanco) {
        this.valorNoBanco = valorNoBanco;
    }

    @JsonValue
    public String getValorNoBanco() {
        return valorNoBanco;
    }

    @JsonCreator
    public static TipoMovimentacao deValorNoBanco(String valor) {
        for (TipoMovimentacao tipo : values()) {
            if (tipo.valorNoBanco.equals(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de movimentação desconhecido: " + valor);
    }
}
