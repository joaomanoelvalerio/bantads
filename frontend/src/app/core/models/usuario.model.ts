export type PerfilUsuario = 'CLIENTE' | 'GERENTE';

export interface Usuario {
  cpf: string;
  nome: string;
  email: string;
}

export interface Credenciais {
  email: string;
  senha: string;
}

export interface RespostaLogin {
  auth: boolean;
  token: string;
  tipo: PerfilUsuario;
  usuario: Usuario;
}
