export interface LinkHateoas {
  href: string;
}

export interface Conta {
  numero: string;
  cpfCliente: string;
  cpfGerente: string;
  saldo: string;
  dataCriacao: string;
  _links?: Record<string, LinkHateoas>;
}
