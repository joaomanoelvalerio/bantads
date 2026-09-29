package com.br.orquestrador.saga;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica um comando de SAGA numa fila `ms.*.cmd` e aguarda a resposta
 * correlata em `orquestrador.reply` — timeout de 30s por passo
 * (docs/specs/05-nao-funcionais/09-sagas-api-compositions.md). A correlação é
 * por (sagaId, tipo): dentro de uma mesma SAGA, cada passo usa um `tipo` de
 * comando diferente, então o par é suficiente para casar comando com
 * resposta na fila compartilhada.
 *
 * <p>Correlação em memória (não sobrevive a um restart do Orquestrador nem
 * escala a múltiplas réplicas) — aceitável no escopo deste projeto de curso,
 * uma única instância.
 */
@Component
public class ComandoSagaPublicador {

    private static final Logger log = LoggerFactory.getLogger(ComandoSagaPublicador.class);

    private final RabbitTemplate rabbitTemplate;
    private final Map<String, CompletableFuture<RespostaSaga>> pendentes = new ConcurrentHashMap<>();

    public ComandoSagaPublicador(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Publica e bloqueia a thread da SAGA (uma por execução) até a resposta ou o timeout do passo. */
    public RespostaSaga enviarEAguardar(String fila, String sagaId, String tipo, Map<String, Object> payload, Duration timeout) {
        String chave = chave(sagaId, tipo);
        CompletableFuture<RespostaSaga> futuro = new CompletableFuture<>();
        pendentes.put(chave, futuro);

        try {
            publicar(fila, sagaId, tipo, payload);
            return futuro.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Timeout de {}s aguardando resposta do passo {} na saga {}", timeout.toSeconds(), tipo, sagaId);
            return falha(sagaId, tipo, "Tempo esgotado aguardando resposta do passo " + tipo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return falha(sagaId, tipo, "Execução interrompida aguardando o passo " + tipo);
        } catch (ExecutionException e) {
            return falha(sagaId, tipo, "Falha inesperada aguardando o passo " + tipo + ": " + e.getMessage());
        } finally {
            pendentes.remove(chave);
        }
    }

    /** Fire-and-forget — usado só para `ms.email.cmd`, que não responde em `orquestrador.reply`. */
    public void enviarSemAguardar(String fila, String sagaId, String tipo, Map<String, Object> payload) {
        publicar(fila, sagaId, tipo, payload);
    }

    /** Chamado pelo listener de `orquestrador.reply` quando uma resposta chega. */
    void completar(RespostaSaga resposta) {
        CompletableFuture<RespostaSaga> futuro = pendentes.remove(chave(resposta.getSagaId(), resposta.getTipo()));
        if (futuro != null) {
            futuro.complete(resposta);
        }
        // futuro == null: resposta chegou depois do timeout do passo (ou é duplicada, entrega
        // at-least-once) — o passo já foi tratado como FALHA e a compensação já pode ter
        // começado; nada mais a fazer com uma resposta tardia.
    }

    private void publicar(String fila, String sagaId, String tipo, Map<String, Object> payload) {
        ComandoSaga comando = new ComandoSaga(sagaId, tipo, Instant.now().toString(), payload);
        rabbitTemplate.convertAndSend(fila, comando);
    }

    private RespostaSaga falha(String sagaId, String tipo, String erro) {
        RespostaSaga resposta = new RespostaSaga();
        resposta.setSagaId(sagaId);
        resposta.setTipo(tipo);
        resposta.setStatus("FALHA");
        resposta.setErro(erro);
        return resposta;
    }

    private String chave(String sagaId, String tipo) {
        return sagaId + "|" + tipo;
    }
}
