package com.br.orquestrador.saga;

import java.util.Map;

/** Formato padrão de resposta de SAGA, publicada em `orquestrador.reply`. */
public class RespostaSaga {
    private String sagaId;
    private String tipo;
    private Map<String, Object> payload;
    private String timestamp;
    private String status;
    private String erro;

    public RespostaSaga() {
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

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }

    public boolean sucesso() {
        return "SUCESSO".equals(status);
    }
}
