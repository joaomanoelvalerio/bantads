import { Injectable, computed, signal } from '@angular/core';
import { Sessao, ehSessao } from '../models/sessao.model';
import { PerfilUsuario, Usuario } from '../models/usuario.model';

const CHAVE_SESSAO = 'bantads.sessao';

/**
 * Guarda a sessão devolvida pelo login em sessionStorage, sob a chave `bantads.sessao`.
 * A senha nunca é persistida. A validade do token é decidida pelo Gateway: o front
 * apenas reage ao 401, sem inspecionar o JWT.
 */
@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly sessao = signal<Sessao | null>(null);

  readonly usuario = computed<Usuario | null>(() => this.sessao()?.usuario ?? null);
  readonly perfil = computed<PerfilUsuario | null>(() => this.sessao()?.tipo ?? null);
  readonly token = computed<string | null>(() => this.sessao()?.token ?? null);
  readonly estaAutenticado = computed<boolean>(() => this.sessao() !== null);

  /**
   * Lê a sessão persistida. Chamada na inicialização da aplicação, antes da primeira
   * resolução de rota, para que um reload em rota protegida não derrube o usuário.
   * Conteúdo corrompido ou incompleto é descartado como ausência de sessão.
   */
  restaurar(): void {
    const bruto = sessionStorage.getItem(CHAVE_SESSAO);

    if (bruto === null) {
      return;
    }

    const recuperada = converter(bruto);

    if (recuperada === null) {
      sessionStorage.removeItem(CHAVE_SESSAO);
      return;
    }

    this.sessao.set(recuperada);
  }

  gravar(sessao: Sessao): void {
    const persistivel: Sessao = {
      token: sessao.token,
      tipo: sessao.tipo,
      usuario: {
        cpf: sessao.usuario.cpf,
        nome: sessao.usuario.nome,
        email: sessao.usuario.email,
      },
    };

    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(persistivel));
    this.sessao.set(persistivel);
  }

  limpar(): void {
    sessionStorage.removeItem(CHAVE_SESSAO);
    this.sessao.set(null);
  }
}

function converter(bruto: string): Sessao | null {
  try {
    const convertido: unknown = JSON.parse(bruto);
    return ehSessao(convertido) ? convertido : null;
  } catch {
    return null;
  }
}
