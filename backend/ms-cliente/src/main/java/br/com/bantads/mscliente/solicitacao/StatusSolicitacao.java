package br.com.bantads.mscliente.solicitacao;

/** Espelha o CHECK constraint de ms_cliente.solicitacoes.status. */
public enum StatusSolicitacao {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    NAO_APROVADO("Não aprovado");

    private final String valorNoBanco;

    StatusSolicitacao(String valorNoBanco) {
        this.valorNoBanco = valorNoBanco;
    }

    public String getValorNoBanco() {
        return valorNoBanco;
    }

    public static StatusSolicitacao deValorNoBanco(String valor) {
        for (StatusSolicitacao status : values()) {
            if (status.valorNoBanco.equals(valor)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de solicitação desconhecido: " + valor);
    }
}
