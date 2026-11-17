package br.com.bantads.mscliente.solicitacao;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Duas grafias deliberadamente diferentes: a coluna do Postgres (CHECK
 * constraint em ms_cliente.solicitacoes.status) usa o literal em português
 * ("Pendente", "Não aprovado" — ver {@link StatusSolicitacaoConverter}, que
 * chama {@link #getValorNoBanco()}), mas o contrato da API (suíte oficial de
 * testes) espera maiúsculo sem acento ("PENDENTE", "NAO_APROVADA") — por
 * isso @JsonValue/@JsonCreator usam um literal próprio
 * ({@link #getValorNoContrato()}/{@link #deValorNoContrato(String)}), não o
 * do banco. Mudar um não exige mudar o outro nem a seed do Postgres.
 */
public enum StatusSolicitacao {
    PENDENTE("Pendente", "PENDENTE"),
    APROVADO("Aprovado", "APROVADO"),
    NAO_APROVADO("Não aprovado", "NAO_APROVADA");

    private final String valorNoBanco;
    private final String valorNoContrato;

    StatusSolicitacao(String valorNoBanco, String valorNoContrato) {
        this.valorNoBanco = valorNoBanco;
        this.valorNoContrato = valorNoContrato;
    }

    /** Usado só por {@link StatusSolicitacaoConverter} — nunca pelo JSON. */
    public String getValorNoBanco() {
        return valorNoBanco;
    }

    public static StatusSolicitacao deValorNoBanco(String valor) {
        for (StatusSolicitacao status : values()) {
            if (status.valorNoBanco.equals(valor)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de solicitação desconhecido (banco): " + valor);
    }

    @JsonValue
    public String getValorNoContrato() {
        return valorNoContrato;
    }

    @JsonCreator
    public static StatusSolicitacao deValorNoContrato(String valor) {
        for (StatusSolicitacao status : values()) {
            if (status.valorNoContrato.equals(valor)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de solicitação desconhecido (contrato): " + valor);
    }
}
