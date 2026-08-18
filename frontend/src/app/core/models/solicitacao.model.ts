export interface Endereco {
  cep: string;
  logradouro: string;
  numero: string;
  complemento: string;
  cidade: string;
  uf: string;
}

export interface NovoCliente extends Endereco {
  nome: string;
  email: string;
  cpf: string;
  telefone: string;
  salario: string;
}

export interface Solicitacao extends NovoCliente {
  _links?: unknown;
}
