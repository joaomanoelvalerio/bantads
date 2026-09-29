package br.com.bantads.mscliente.saga;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Formato padrão de comando de SAGA (docs/specs/05-nao-funcionais/07-rabbitmq-filas.md). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComandoSaga {
    private String sagaId;
    private String tipo;
    private String timestamp;
    private Map<String, Object> payload;
}
