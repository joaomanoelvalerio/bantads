package br.com.bantads.mscliente.solicitacao;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Espelha o CHECK constraint de ms_cliente.solicitacoes.status. @JsonValue/
 * @JsonCreator fazem o JSON trafegar com o mesmo literal em português do
 * banco ("Pendente", não "PENDENTE") — sem isso o Jackson usaria o nome Java
 * do enum por padrão, divergindo do resto do contrato (R8 etc.).
 */
public enum StatusSolicitacao {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    NAO_APROVADO("Não aprovado");

    private final String valorNoBanco;

    StatusSolicitacao(String valorNoBanco) {
        this.valorNoBanco = valorNoBanco;
    }

    @JsonValue
    public String getValorNoBanco() {
        return valorNoBanco;
    }

    @JsonCreator
    public static StatusSolicitacao deValorNoBanco(String valor) {
        for (StatusSolicitacao status : values()) {
            if (status.valorNoBanco.equals(valor)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de solicitação desconhecido: " + valor);
    }
}
