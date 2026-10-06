package br.com.bantads.mscliente.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ComandoProcessadoId implements Serializable {

    @Column(name = "saga_id")
    private String sagaId;

    private String tipo;

    public ComandoProcessadoId(String sagaId, String tipo) {
        this.sagaId = sagaId;
        this.tipo = tipo;
    }
}
