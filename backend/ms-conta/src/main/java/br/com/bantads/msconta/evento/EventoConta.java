package br.com.bantads.msconta.evento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Espelha ms_conta.eventos_conta — o event store, lado COMMAND do CQRS
 * (backend/ms-conta/db/01-schema.sql). Mapeada já na S2 para validar a conexão
 * com o schema; append/replay real (fold ordenado por versao) é trabalho da S4
 * (ver docs/design/modelagem-ct.md).
 */
@Entity
@Table(schema = "ms_conta", name = "eventos_conta")
@Getter
@Setter
@NoArgsConstructor
public class EventoConta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "objeto_id")
    private String objetoId;

    private TipoEvento tipo;

    /** JSON bruto do payload — valores monetários sempre como string, nunca number. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    private Integer versao;

    // Nome de coluna reservado no SQL padrão — precisa ir entre aspas na DDL e aqui.
    @Column(name = "`timestamp`")
    private OffsetDateTime timestamp;
}
