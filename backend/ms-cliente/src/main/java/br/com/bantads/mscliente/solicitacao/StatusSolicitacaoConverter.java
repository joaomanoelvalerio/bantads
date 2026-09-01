package br.com.bantads.mscliente.solicitacao;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** A coluna guarda os literais em português ("Pendente", "Não aprovado") — não o nome do enum. */
@Converter(autoApply = true)
public class StatusSolicitacaoConverter implements AttributeConverter<StatusSolicitacao, String> {

    @Override
    public String convertToDatabaseColumn(StatusSolicitacao status) {
        return status == null ? null : status.getValorNoBanco();
    }

    @Override
    public StatusSolicitacao convertToEntityAttribute(String valor) {
        return valor == null ? null : StatusSolicitacao.deValorNoBanco(valor);
    }
}
