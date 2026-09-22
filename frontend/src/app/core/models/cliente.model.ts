import { NovoCliente } from './solicitacao.model';

/**
 * Cliente já aprovado, como devolvido por `GET /clientes/{cpf}` — o recurso que a
 * SAGA de aprovação (R9) cria e que o job aponta por `resultType: "resource"`.
 */
export interface Cliente extends NovoCliente {
  _links?: unknown;
}

/**
 * Cliente como listado em `GET /clientes` (R11). `saldo` é `null` para cliente
 * sem conta (ou ausente), caso em que a linha exibe a ausência em vez de quebrar.
 */
export interface ClienteListado {
  cpf: string;
  nome: string;
  cidade: string;
  estado: string;
  saldo?: string | null;
  _links?: unknown;
}
