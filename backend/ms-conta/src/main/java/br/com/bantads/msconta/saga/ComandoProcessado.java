package br.com.bantads.msconta.saga;

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

/**
 * Idempotência por (sagaId, tipo),
 * S8. Guarda só os comandos que CRIAM algo (`conta.criar`): a chave
 * primária composta é o próprio mecanismo de deduplicação — uma segunda
 * tentativa de inserir a mesma (sagaId, tipo) falha com violação de
 * unicidade, capturada pelo listener pra devolver a resposta já salva em
 * vez de repetir a criação.
 */
@Entity
@Table(schema = "ms_conta", name = "comandos_processados")
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
