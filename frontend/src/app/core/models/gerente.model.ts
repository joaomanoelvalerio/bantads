/** Gerente ativo, como devolvido por `GET /gerentes` e `GET /gerentes/{cpf}`. */
export interface Gerente {
  cpf: string;
  nome: string;
  email: string;
  telefone: string;
  /** Contagem calculada pelo back-end; o front apenas exibe. */
  quantidadeClientes: number;
  _links?: unknown;
}

/**
 * Corpo de `POST /gerentes` (R13). A senha é definida aqui e não por e-mail, ao
 * contrário da aprovação de cliente; o objeto existe só durante o envio.
 */
export interface NovoGerente {
  nome: string;
  email: string;
  cpf: string;
  telefone: string;
  senha: string;
}

/** Corpo de `PUT /gerentes/{cpf}` (R14). CPF e e-mail não são alteráveis. */
export interface AlteracaoDeGerente {
  nome: string;
  telefone: string;
}
