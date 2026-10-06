package br.com.bantads.mscliente.saga;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "ms_cliente", name = "comandos_processados")
@Getter
@Setter
@NoArgsConstructor
public class ComandoProcessado {

    @EmbeddedId
    private ComandoProcessadoId id;

    @JdbcTypeCode(SqlTypes.JSON)
    private String resposta;

    @Column(name = "processado_em")
    private OffsetDateTime processadoEm;

    public ComandoProcessado(String sagaId, String tipo, String resposta) {
        this.id = new ComandoProcessadoId(sagaId, tipo);
        this.resposta = resposta;
        this.processadoEm = OffsetDateTime.now();
    }
}
