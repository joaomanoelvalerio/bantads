import { PerfilUsuario, Usuario } from './usuario.model';

export interface Sessao {
  token: string;
  tipo: PerfilUsuario;
  usuario: Usuario;
}

const PERFIS: readonly PerfilUsuario[] = ['CLIENTE', 'GERENTE'];

export function ehSessao(valor: unknown): valor is Sessao {
  if (valor === null || typeof valor !== 'object') {
    return false;
  }

  const candidata = valor as Record<string, unknown>;
  const usuario = candidata['usuario'];

  return (
    ehTextoPreenchido(candidata['token']) &&
    PERFIS.includes(candidata['tipo'] as PerfilUsuario) &&
    usuario !== null &&
    typeof usuario === 'object' &&
    ehTextoPreenchido((usuario as Record<string, unknown>)['cpf']) &&
    ehTextoPreenchido((usuario as Record<string, unknown>)['nome']) &&
    ehTextoPreenchido((usuario as Record<string, unknown>)['email'])
  );
}

function ehTextoPreenchido(valor: unknown): valor is string {
  return typeof valor === 'string' && valor.trim().length > 0;
}
