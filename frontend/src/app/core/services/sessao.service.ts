import { Injectable, computed, signal } from '@angular/core';
import { Sessao, ehSessao } from '../models/sessao.model';
import { PerfilUsuario, Usuario } from '../models/usuario.model';

const CHAVE_SESSAO = 'bantads.sessao';

@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly sessao = signal<Sessao | null>(recuperarDoArmazenamento());

  readonly usuario = computed<Usuario | null>(() => this.sessao()?.usuario ?? null);

  gravar(sessao: Sessao): void {
    localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
    this.sessao.set(sessao);
  }

  limpar(): void {
    localStorage.removeItem(CHAVE_SESSAO);
    this.sessao.set(null);
  }

  token(): string | null {
    return this.sessao()?.token ?? null;
  }

  perfil(): PerfilUsuario | null {
    return this.sessao()?.tipo ?? null;
  }

  estaAutenticado(): boolean {
    return this.sessao() !== null;
  }

  rotaInicial(): string {
    switch (this.perfil()) {
      case 'CLIENTE':
        return '/cliente';
      case 'GERENTE':
        return '/gerente';
      default:
        return '/login';
    }
  }
}

function recuperarDoArmazenamento(): Sessao | null {
  const bruto = localStorage.getItem(CHAVE_SESSAO);
  if (bruto === null) {
    return null;
  }
  try {
    const convertido: unknown = JSON.parse(bruto);
    return ehSessao(convertido) ? convertido : null;
  } catch {
    localStorage.removeItem(CHAVE_SESSAO);
    return null;
  }
}
