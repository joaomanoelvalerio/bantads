import Decimal from 'decimal.js';
import { DateTime } from 'luxon';
import { Extrato, Movimentacao, TipoMovimentacao } from '../../../core/models/movimentacao.model';
import { FUSO_BANTADS } from '../../../shared/formato/data-hora';

/** Intervalo máximo aceito entre a data de início e a data de fim do extrato. */
export const DIAS_MAXIMOS_DO_PERIODO = 365;

export type SentidoDaMovimentacao = 'ENTRADA' | 'SAIDA';

export interface LancamentoDoExtrato {
  horario: string;
  operacao: string;
  sentido: SentidoDaMovimentacao;
  origem: string | null;
  destino: string | null;
  valor: string;
  saldoApos: string;
}

export interface DiaDoExtrato {
  chave: string;
  rotulo: string;
  lancamentos: readonly LancamentoDoExtrato[];
  saldoConsolidado: string;
}

const NOME_DA_OPERACAO: Record<TipoMovimentacao, string> = {
  DEPOSITO: 'Depósito',
  SAQUE: 'Saque',
  TRANSFERENCIA: 'Transferência',
};

/** Data de hoje no fuso do banco, sem hora. */
export function hojeNoFusoDoBanco(): DateTime {
  return DateTime.now().setZone(FUSO_BANTADS).startOf('day');
}

/** Converte um valor `yyyy-MM-dd` vindo do formulário para o fuso do banco. */
export function diaDeIso(iso: string): DateTime {
  return DateTime.fromISO(iso, { zone: FUSO_BANTADS }).startOf('day');
}

/**
 * Monta a linha do tempo diária a partir do saldo de abertura devolvido pelo
 * back-end. Todo dia do período aparece, inclusive os sem movimentação, que apenas
 * repetem o saldo consolidado do dia anterior. O acúmulo é sempre com Decimal.
 */
export function montarLinhaDoTempo(
  extrato: Extrato,
  cpfDoCliente: string,
  dataInicio: DateTime,
  dataFim: DateTime,
): DiaDoExtrato[] {
  const porDia = agruparPorDia(extrato.movimentacoes);
  const ultimoDia = dataFim.startOf('day').toMillis();

  let saldo = new Decimal(extrato.saldoAnterior);
  const dias: DiaDoExtrato[] = [];

  for (
    let dia = dataInicio.startOf('day');
    dia.toMillis() <= ultimoDia;
    dia = dia.plus({ days: 1 })
  ) {
    const chave = dia.toFormat('yyyy-LL-dd');
    const lancamentos: LancamentoDoExtrato[] = [];

    for (const { momento, movimentacao } of porDia.get(chave) ?? []) {
      const sentido = sentidoDe(movimentacao, cpfDoCliente);
      const valor = new Decimal(movimentacao.valor);

      saldo = sentido === 'ENTRADA' ? saldo.plus(valor) : saldo.minus(valor);

      lancamentos.push({
        horario: momento.toFormat('HH:mm'),
        operacao: NOME_DA_OPERACAO[movimentacao.tipo],
        sentido,
        origem: participante(movimentacao.nomeOrigem, movimentacao.cpfOrigem),
        destino: participante(movimentacao.nomeDestino, movimentacao.cpfDestino),
        valor: valor.toFixed(2),
        saldoApos: saldo.toFixed(2),
      });
    }

    dias.push({
      chave,
      rotulo: dia.setLocale('pt-BR').toFormat("cccc, dd 'de' LLLL 'de' yyyy"),
      lancamentos,
      saldoConsolidado: saldo.toFixed(2),
    });
  }

  return dias;
}

interface MovimentacaoDatada {
  momento: DateTime;
  movimentacao: Movimentacao;
}

function agruparPorDia(movimentacoes: readonly Movimentacao[]): Map<string, MovimentacaoDatada[]> {
  const datadas = movimentacoes
    .map((movimentacao) => ({
      momento: DateTime.fromISO(movimentacao.dataHora, { zone: FUSO_BANTADS }),
      movimentacao,
    }))
    .filter(({ momento }) => momento.isValid)
    .sort((uma, outra) => uma.momento.toMillis() - outra.momento.toMillis());

  const porDia = new Map<string, MovimentacaoDatada[]>();

  for (const datada of datadas) {
    const chave = datada.momento.toFormat('yyyy-LL-dd');
    const doDia = porDia.get(chave);

    if (doDia === undefined) {
      porDia.set(chave, [datada]);
    } else {
      doDia.push(datada);
    }
  }

  return porDia;
}

/**
 * O sentido vem da comparação do CPF da sessão com os CPFs de origem e destino,
 * nunca do tipo isolado: a mesma transferência é saída para quem enviou e entrada
 * para quem recebeu.
 */
export function sentidoDe(movimentacao: Movimentacao, cpfDoCliente: string): SentidoDaMovimentacao {
  const cliente = digitos(cpfDoCliente);

  if (digitos(movimentacao.cpfDestino) === cliente && cliente.length > 0) {
    return 'ENTRADA';
  }

  if (digitos(movimentacao.cpfOrigem) === cliente && cliente.length > 0) {
    return 'SAIDA';
  }

  return movimentacao.tipo === 'DEPOSITO' ? 'ENTRADA' : 'SAIDA';
}

function participante(
  nome: string | null | undefined,
  cpf: string | null | undefined,
): string | null {
  if (typeof nome === 'string' && nome.trim().length > 0) {
    return nome.trim();
  }

  return typeof cpf === 'string' && cpf.trim().length > 0 ? cpf.trim() : null;
}

function digitos(valor: string | null | undefined): string {
  return typeof valor === 'string' ? valor.replace(/\D/g, '') : '';
}
