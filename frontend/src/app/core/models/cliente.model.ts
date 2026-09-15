import { NovoCliente } from './solicitacao.model';

/**
 * Cliente já aprovado, como devolvido por `GET /clientes/{cpf}` — o recurso que a
 * SAGA de aprovação (R9) cria e que o job aponta por `resultType: "resource"`.
 */
export interface Cliente extends NovoCliente {
  _links?: unknown;
}
