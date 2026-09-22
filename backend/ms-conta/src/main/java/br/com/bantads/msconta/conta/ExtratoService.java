package br.com.bantads.msconta.conta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * R7 — Consulta de extrato (docs/specs/02-requisitos-funcionais.md). Servido
 * inteiramente pelo lado QUERY (contas/movimentacoes): ao contrário da
 * validação de saldo de R5/R6, aqui não é o replay do command que importa —
 * é justamente o read model que o front consome. saldoAbertura é o fold das
 * movimentações anteriores à data inicial; o front monta a timeline diária
 * (saldo acumulado) com Luxon a partir daí.
 */
@Service
public class ExtratoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int PADRAO_DIAS = 30;
    private static final int MAXIMO_DIAS = 365;

    private final ContaService contaService;
    private final MovimentacaoRepository movimentacaoRepository;

    public ExtratoService(ContaService contaService, MovimentacaoRepository movimentacaoRepository) {
        this.contaService = contaService;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public ExtratoResponse consultar(String numeroConta, String cpfSolicitante, LocalDate dataInicio, LocalDate dataFim) {
        Conta conta = contaService.buscarEVerificarPosse(numeroConta, cpfSolicitante);

        LocalDate fim = dataFim != null ? dataFim : LocalDate.now(FUSO);
        LocalDate inicio = dataInicio != null ? dataInicio : fim.minusDays(PADRAO_DIAS - 1);

        if (inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data início não pode ser depois da data fim");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > MAXIMO_DIAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intervalo máximo permitido é de 365 dias");
        }

        OffsetDateTime corteInicio = inicio.atStartOfDay(FUSO).toOffsetDateTime();
        OffsetDateTime corteFimExclusivo = fim.plusDays(1).atStartOfDay(FUSO).toOffsetDateTime();

        List<Movimentacao> todas = movimentacaoRepository.findByNumeroContaOrderByDataHoraAsc(numeroConta);

        BigDecimal saldoAbertura = BigDecimal.ZERO;
        List<Movimentacao> doPeriodo = new ArrayList<>();
        for (Movimentacao movimentacao : todas) {
            if (movimentacao.getDataHora().isBefore(corteInicio)) {
                saldoAbertura = saldoAbertura.add(efeito(movimentacao, conta.getCpfCliente()));
            } else if (movimentacao.getDataHora().isBefore(corteFimExclusivo)) {
                doPeriodo.add(movimentacao);
            }
        }

        return new ExtratoResponse(numeroConta, inicio, fim, saldoAbertura, doPeriodo);
    }

    /** Sinal da movimentação do ponto de vista do titular desta conta. */
    private BigDecimal efeito(Movimentacao movimentacao, String cpfTitular) {
        return switch (movimentacao.getTipo()) {
            case DEPOSITO -> movimentacao.getValor();
            case SAQUE -> movimentacao.getValor().negate();
            case TRANSFERENCIA -> cpfTitular.equals(movimentacao.getCpfDestino())
                    ? movimentacao.getValor()
                    : movimentacao.getValor().negate();
        };
    }
}
