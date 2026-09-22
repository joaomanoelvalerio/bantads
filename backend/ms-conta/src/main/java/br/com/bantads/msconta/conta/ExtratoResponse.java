package br.com.bantads.msconta.conta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * R7 — o front-end monta a timeline diária (saldo acumulado) iterando os
 * dias com Luxon a partir de saldoAbertura + movimentacoes
 * (docs/specs/02-requisitos-funcionais.md).
 */
public record ExtratoResponse(
        String numeroConta, LocalDate dataInicio, LocalDate dataFim, BigDecimal saldoAbertura, List<Movimentacao> movimentacoes) {
}
