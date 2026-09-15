/**
 * Operações demoradas do back-end (SAGAs) respondem 202 Accepted com um jobId em
 * vez do resultado. O job vive no Redis com TTL de 5 minutos e o front consulta
 * `GET /jobs/{jobId}/status` até o job chegar a um estado final.
 */

/** Estado do job no back-end. Só CONCLUIDO e FALHA são finais. */
export type SituacaoDoJob = 'PENDENTE' | 'CONCLUIDO' | 'FALHA';

/**
 * Onde buscar o resultado de um job concluído: `resource` aponta para
 * `GET /{dominio}/{resourceId}`; `inline` para `GET /jobs/{jobId}/result`.
 */
export type FormatoDoResultado = 'resource' | 'inline';

/**
 * Corpo do 202 devolvido pela operação que dispara o job. O jobId vem daqui e de
 * mais lugar nenhum: nem de header, nem deduzido do recurso operado.
 */
export interface RespostaAceita {
  jobId: string;
  _links?: unknown;
}

/** Corpo de `GET /jobs/{jobId}/status`. */
export interface StatusDoJob {
  jobId: string;
  status: SituacaoDoJob;
  resultType: FormatoDoResultado | null;
  dominio: string;
  resourceId: string | null;
  /**
   * O status ecoa o resultado quando o formato é `inline`. Tipado para registrar
   * a presença; o valor usado é sempre o de `GET /jobs/{jobId}/result`, que é o
   * endereço definido pelo contrato.
   */
  resultado?: unknown;
  erro: string | null;
  _links?: unknown;
}

/**
 * Como o acompanhamento de um job termina, do ponto de vista da tela. Nenhum dos
 * três é exceção: tempo esgotado em particular não é sucesso nem falha, e quem
 * chama precisa ser obrigado pelo tipo a decidir o que dizer ao usuário.
 */
export type DesfechoDoJob<T> =
  | { tipo: 'CONCLUIDO'; resultado: T }
  | { tipo: 'FALHA'; mensagem: string }
  | { tipo: 'TEMPO_ESGOTADO' };
