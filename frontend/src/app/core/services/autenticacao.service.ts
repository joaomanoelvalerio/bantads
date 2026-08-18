import { Injectable, inject } from '@angular/core';
import { Observable, finalize, map, tap } from 'rxjs';
import { Sessao } from '../models/sessao.model';
import { Credenciais, RespostaLogin } from '../models/usuario.model';
import { ApiService } from './api.service';
import { SessaoService } from './sessao.service';

@Injectable({ providedIn: 'root' })
export class AutenticacaoService {
  private readonly api = inject(ApiService);
  private readonly sessaoService = inject(SessaoService);

  entrar(credenciais: Credenciais): Observable<Sessao> {
    return this.api.post<RespostaLogin>('/login', credenciais).pipe(
      map(({ token, tipo, usuario }) => ({ token, tipo, usuario })),
      tap((sessao) => this.sessaoService.gravar(sessao)),
    );
  }

  sair(): Observable<void> {
    return this.api
      .post<void>('/logout', {})
      .pipe(finalize(() => this.sessaoService.limpar()));
  }
}
