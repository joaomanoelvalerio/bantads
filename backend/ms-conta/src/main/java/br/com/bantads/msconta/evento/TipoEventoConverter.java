package br.com.bantads.msconta.evento;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** A coluna guarda os literais grafados na especificação (com acento) — não o nome do enum. */
@Converter(autoApply = true)
public class TipoEventoConverter implements AttributeConverter<TipoEvento, String> {

    @Override
    public String convertToDatabaseColumn(TipoEvento tipo) {
        return tipo == null ? null : tipo.getValorNoBanco();
    }

    @Override
    public TipoEvento convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoEvento.deValorNoBanco(valor);
    }
}
