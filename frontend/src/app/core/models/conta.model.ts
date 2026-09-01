export interface LinkHateoas {
  href: string;
}

export interface Conta {
  numero: string;
  cpf: string;
  saldo: string;
  dataCriacao: string;
  _links?: Record<string, LinkHateoas>;
}
