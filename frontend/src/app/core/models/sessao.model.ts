import { PerfilUsuario, Usuario } from './usuario.model';

export interface Sessao {
  token: string;
  tipo: PerfilUsuario;
  usuario: Usuario;
}

export function ehSessao(valor: unknown): valor is Sessao {
  if (valor === null || typeof valor !== 'object') {
    return false;
  }
  const candidata = valor as Record<string, unknown>;
  const usuario = candidata['usuario'] as Record<string, unknown> | undefined;
  const perfis: PerfilUsuario[] = ['CLIENTE', 'GERENTE'];

  return (
    typeof candidata['token'] === 'string' &&
    perfis.includes(candidata['tipo'] as PerfilUsuario) &&
    usuario !== undefined &&
    typeof usuario['cpf'] === 'string' &&
    typeof usuario['nome'] === 'string' &&
    typeof usuario['email'] === 'string'
  );
}
