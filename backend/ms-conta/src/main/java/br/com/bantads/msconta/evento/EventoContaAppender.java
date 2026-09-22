package br.com.bantads.msconta.evento;

import java.time.OffsetDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uma tentativa de append = uma transação própria (REQUIRES_NEW por padrão,
 * mas aqui basta o default do Spring já que cada chamada externa começa sem
 * transação aberta). Fica separado de EventoContaService para que o retry em
 * caso de conflito de versão (ver EventoContaService) comece sempre com um
 * EntityManager novo — depois que um flush falha, a transação/persistence
 * context correntes ficam inutilizáveis para novas tentativas.
 */
@Component
public class EventoContaAppender {

    private final EventoContaRepository eventoContaRepository;
    private final ApplicationEventPublisher publisher;

    public EventoContaAppender(EventoContaRepository eventoContaRepository, ApplicationEventPublisher publisher) {
        this.eventoContaRepository = eventoContaRepository;
        this.publisher = publisher;
    }

    @Transactional
    public EventoConta tentarRegistrar(String numeroConta, TipoEvento tipo, String payloadJson) {
        int proximaVersao = eventoContaRepository.buscarUltimaVersao(numeroConta) + 1;

        EventoConta evento = new EventoConta();
        evento.setObjetoId(numeroConta);
        evento.setTipo(tipo);
        evento.setPayload(payloadJson);
        evento.setVersao(proximaVersao);
        evento.setTimestamp(OffsetDateTime.now());

        // unique(objeto_id, versao) resolve a corrida: se outra transação já
        // gravou essa versão primeiro, o flush lança DataIntegrityViolationException
        // e quem chamou (EventoContaService) refaz a tentativa com versão nova.
        EventoConta salvo = eventoContaRepository.saveAndFlush(evento);
        publisher.publishEvent(new EventoContaRegistradoEvent(salvo.getId()));
        return salvo;
    }

    /**
     * R6 — transferência: os dois eventos (origem e destino) são gravados numa
     * única transação local (não é SAGA — docs/specs/05-nao-funcionais/10-cqrs.md).
     * Um conflito de versão em qualquer um dos dois lados derruba a transação
     * inteira; quem chamou (OperacaoContaService) refaz as DUAS tentativas
     * juntas, recalculando as próximas versões de ambas as contas.
     */
    @Transactional
    public ParTransferencia tentarRegistrarTransferencia(
            String numeroContaOrigem, String payloadOrigemJson, String numeroContaDestino, String payloadDestinoJson) {
        OffsetDateTime agora = OffsetDateTime.now();

        EventoConta origem = new EventoConta();
        origem.setObjetoId(numeroContaOrigem);
        origem.setTipo(TipoEvento.TRANSFERENCIA_ORIGEM);
        origem.setPayload(payloadOrigemJson);
        origem.setVersao(eventoContaRepository.buscarUltimaVersao(numeroContaOrigem) + 1);
        origem.setTimestamp(agora);

        EventoConta destino = new EventoConta();
        destino.setObjetoId(numeroContaDestino);
        destino.setTipo(TipoEvento.TRANSFERENCIA_DESTINO);
        destino.setPayload(payloadDestinoJson);
        destino.setVersao(eventoContaRepository.buscarUltimaVersao(numeroContaDestino) + 1);
        destino.setTimestamp(agora);

        EventoConta origemSalvo = eventoContaRepository.saveAndFlush(origem);
        EventoConta destinoSalvo = eventoContaRepository.saveAndFlush(destino);

        publisher.publishEvent(new EventoContaRegistradoEvent(origemSalvo.getId()));
        publisher.publishEvent(new EventoContaRegistradoEvent(destinoSalvo.getId()));

        return new ParTransferencia(origemSalvo, destinoSalvo);
    }
}
