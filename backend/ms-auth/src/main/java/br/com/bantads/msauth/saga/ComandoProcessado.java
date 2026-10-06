package br.com.bantads.msauth.saga;

import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Idempotência por (sagaId, tipo),
 * S8. Guarda só `auth.criar-credencial` — reentrega at-least-once não pode
 * criar uma segunda credencial (e geraria uma senha ALEATÓRIA diferente da
 * primeira, pior ainda); os demais comandos deste serviço já são
 * idempotentes por construção. Índice único composto em (sagaId, tipo) —
 * `spring.data.mongodb.auto-index-creation: true` já está ligado desde a
 * Semana 03, então ele é criado de verdade no boot.
 */
@Document(collection = "comandos_processados")
@CompoundIndex(def = "{'sagaId': 1, 'tipo': 1}", unique = true, name = "uk_saga_tipo")
@Getter
@Setter
@NoArgsConstructor
public class ComandoProcessado {

    @Id
    private String id;

    private String sagaId;

    private String tipo;

    private Map<String, Object> resposta;

    public ComandoProcessado(String sagaId, String tipo, Map<String, Object> resposta) {
        this.sagaId = sagaId;
        this.tipo = tipo;
        this.resposta = resposta;
    }
}
