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

export type StatusSolicitacao = 'PENDENTE' | 'APROVADO' | 'NAO_APROVADO';

/**
 * Solicitação como chega em `GET /solicitacoes` (R8). Mais estreita que a
 * `Solicitacao` devolvida pelo autocadastro: a listagem do gerente não traz
 * endereço nem telefone.
 *
 * Os campos vêm como string porque as duas fontes de contrato discordam na
 * grafia: o schema do ms-cliente grava `Pendente`/`Aprovado`/`Não aprovado` e
 * chama a data de `decidido_em`, enquanto a especificação de R8 usa `PENDENTE` e
 * `dataHoraAnalise`. A normalização abaixo aceita as duas.
 */
export interface SolicitacaoDto {
  cpf: string;
  nome: string;
  email: string;
  salario: string;
  status: string;
  motivo?: string | null;
  dataHoraAnalise?: string | null;
  decididoEm?: string | null;
  _links?: unknown;
}

/** Solicitação já normalizada para uso na tela. */
export interface SolicitacaoAnalisada {
  cpf: string;
  nome: string;
  email: string;
  /** Valor monetário como string, nunca convertido para number. */
  salario: string;
  /** `null` quando o back-end devolve um literal de status não previsto. */
  status: StatusSolicitacao | null;
  /** Preenchido apenas quando a solicitação foi recusada. */
  motivo: string | null;
  /** Instante ISO da aprovação ou rejeição; `null` enquanto Pendente. */
  dataHoraAnalise: string | null;
}

export function normalizarSolicitacao(bruta: SolicitacaoDto): SolicitacaoAnalisada {
  return {
    cpf: bruta.cpf,
    nome: bruta.nome,
    email: bruta.email,
    salario: bruta.salario,
    status: statusDaSolicitacao(bruta.status),
    motivo: textoOuNulo(bruta.motivo),
    dataHoraAnalise: textoOuNulo(bruta.dataHoraAnalise) ?? textoOuNulo(bruta.decididoEm),
  };
}

/** Reduz as grafias aceitas a um único literal. `null` para o que não reconhece. */
export function statusDaSolicitacao(bruto: string): StatusSolicitacao | null {
  const chave = bruto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .trim()
    .toUpperCase()
    .replace(/\s+/g, '_');

  if (chave === 'PENDENTE' || chave === 'APROVADO' || chave === 'NAO_APROVADO') {
    return chave;
  }

  return null;
}

function textoOuNulo(valor: string | null | undefined): string | null {
  return typeof valor === 'string' && valor.trim().length > 0 ? valor : null;
}
