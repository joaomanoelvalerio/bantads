package br.com.bantads.msconta.conta;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoMovimentacaoConverter implements AttributeConverter<TipoMovimentacao, String> {

    @Override
    public String convertToDatabaseColumn(TipoMovimentacao tipo) {
        return tipo == null ? null : tipo.getValorNoBanco();
    }

    @Override
    public TipoMovimentacao convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoMovimentacao.deValorNoBanco(valor);
    }
}
