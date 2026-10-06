package com.br.orquestrador.saga;

import com.br.orquestrador.OrquestradorApplication;
import com.br.orquestrador.saga.aprovarcliente.AprovarClienteSagaService;
import com.br.orquestrador.saga.inserirgerente.InserirGerenteSagaService;
import com.br.orquestrador.saga.removergerente.RemoverGerenteSagaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Ponto de entrada de toda SAGA — o Gateway publica aqui e retorna 202
 * imediatamente (docs/specs/05-nao-funcionais/09-sagas-api-compositions.md).
 * As três SAGAs do enunciado existem: Aprovar Cliente (R9, S6), Inserir
 * Gerente (R13, S7) e Remover Gerente (R15, S8).
 */
@Component
public class SagaCmdListener {

    private static final Logger log = LoggerFactory.getLogger(SagaCmdListener.class);
    private static final String TIPO_APROVAR_CLIENTE = "aprovar-cliente";
    private static final String TIPO_INSERIR_GERENTE = "inserir-gerente";
    private static final String TIPO_REMOVER_GERENTE = "remover-gerente";

    private final AprovarClienteSagaService aprovarClienteSagaService;
    private final InserirGerenteSagaService inserirGerenteSagaService;
    private final RemoverGerenteSagaService removerGerenteSagaService;
    private final JobRepositorio jobRepositorio;

    public SagaCmdListener(
            AprovarClienteSagaService aprovarClienteSagaService,
            InserirGerenteSagaService inserirGerenteSagaService,
            RemoverGerenteSagaService removerGerenteSagaService,
            JobRepositorio jobRepositorio) {
        this.aprovarClienteSagaService = aprovarClienteSagaService;
        this.inserirGerenteSagaService = inserirGerenteSagaService;
        this.removerGerenteSagaService = removerGerenteSagaService;
        this.jobRepositorio = jobRepositorio;
    }

    @RabbitListener(queues = OrquestradorApplication.QUEUE_SAGA_CMD)
    public void aoReceberComandoDeSaga(ComandoSaga comando) {
        log.info("Comando de SAGA recebido: sagaId={} tipo={}", comando.getSagaId(), comando.getTipo());

        try {
            switch (comando.getTipo()) {
                case TIPO_APROVAR_CLIENTE -> aprovarClienteSagaService.executar(
                        comando.getSagaId(), String.valueOf(comando.getPayload().get("cpf")));
                case TIPO_INSERIR_GERENTE -> inserirGerenteSagaService.executar(comando.getSagaId(), comando.getPayload());
                case TIPO_REMOVER_GERENTE -> removerGerenteSagaService.executar(
                        comando.getSagaId(), String.valueOf(comando.getPayload().get("cpf")));
                default -> {
                    log.warn("Tipo de SAGA desconhecido: {}", comando.getTipo());
                    jobRepositorio.marcarFalha(comando.getSagaId(), "Tipo de SAGA desconhecido: " + comando.getTipo());
                }
            }
        } catch (Exception e) {
            log.error("Falha inesperada executando a SAGA {} ({})", comando.getTipo(), comando.getSagaId(), e);
            jobRepositorio.marcarFalha(comando.getSagaId(), "Falha inesperada ao processar a operação: " + e.getMessage());
        }
    }
}
