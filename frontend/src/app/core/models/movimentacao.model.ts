import { LinkHateoas } from './conta.model';

export type TipoMovimentacao = 'DEPOSITO' | 'SAQUE' | 'TRANSFERENCIA';

/**
 * Movimentação como devolvida pelo extrato. Origem e destino só vêm preenchidos em
 * transferência, e o enriquecimento com CPF e nomes é feito pelo API Gateway: o front
 * apenas lê o que chega. Valores monetários trafegam como string.
 */
export interface Movimentacao {
  dataHora: string;
  tipo: TipoMovimentacao;
  cpfOrigem?: string | null;
  nomeOrigem?: string | null;
  cpfDestino?: string | null;
  nomeDestino?: string | null;
  valor: string;
  _links?: Record<string, LinkHateoas>;
}

/**
 * Resposta de GET /contas/{numero}/extrato. O back-end devolve o saldo consolidado
 * anterior à data inicial; a linha do tempo diária é montada pelo front.
 */
export interface Extrato {
  saldoAnterior: string;
  movimentacoes: readonly Movimentacao[];
  _links?: Record<string, LinkHateoas>;
}
