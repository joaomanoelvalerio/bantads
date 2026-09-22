/**
 * Linha do relatório de clientes (R16), lida de `GET /jobs/{jobId}/result`.
 * Valores monetários e número de conta trafegam como string: a conta pode ter
 * zero à esquerda. Os dados de conta e gerente ficam nulos se o cliente não
 * tiver conta.
 */
export interface LinhaDoRelatorio {
  cpf: string;
  nome: string;
  email: string;
  salario: string;
  numeroConta?: string | null;
  saldo?: string | null;
  cpfGerente?: string | null;
  nomeGerente?: string | null;
  _links?: unknown;
}
