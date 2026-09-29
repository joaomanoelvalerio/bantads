package com.br.orquestrador.saga;

import java.util.Map;

/** Formato padrão de comando de SAGA (docs/specs/05-nao-funcionais/07-rabbitmq-filas.md). */
public class ComandoSaga {
    private String sagaId;
    private String tipo;
    private String timestamp;
    private Map<String, Object> payload;

    public ComandoSaga() {
    }

    public ComandoSaga(String sagaId, String tipo, String timestamp, Map<String, Object> payload) {
        this.sagaId = sagaId;
        this.tipo = tipo;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
