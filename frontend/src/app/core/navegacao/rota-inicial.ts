import { PerfilUsuario } from '../models/usuario.model';

export const ROTA_LOGIN = '/login';

/**
 * Fonte única do mapeamento entre perfil e área inicial do usuário.
 * Guards, tela de login e qualquer redirecionamento por perfil devem usar esta função.
 */
export function rotaInicial(perfil: PerfilUsuario | null): string {
  switch (perfil) {
    case 'CLIENTE':
      return '/cliente';
    case 'GERENTE':
      return '/gerente';
    default:
      return ROTA_LOGIN;
  }
}
