package br.com.bantads.msauth.saga;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Formato padrão de resposta de SAGA, publicada em `orquestrador.reply`. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespostaSaga {
    private String sagaId;
    private String tipo;
    private Map<String, Object> payload;
    private String timestamp;
    private String status;
    private String erro;
}
