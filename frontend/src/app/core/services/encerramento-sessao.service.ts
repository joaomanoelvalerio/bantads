import { Injectable, inject } from '@angular/core';
import { Params, Router } from '@angular/router';
import { ROTA_LOGIN } from '../navegacao/rota-inicial';
import { SessaoService } from './sessao.service';

/**
 * Ponto único de encerramento da sessão: descarta os dados locais e devolve o
 * usuário ao login. Requisições simultâneas que falhem com 401 chamam este
 * serviço mais de uma vez, então apenas o primeiro encerramento navega.
 */
@Injectable({ providedIn: 'root' })
export class EncerramentoSessaoService {
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);

  private encerrando = false;

  /** O Gateway respondeu 401 a uma requisição protegida: a sessão não existe mais. */
  porExpiracao(): void {
    this.encerrar({ motivo: 'expirada' });
  }

  /** O usuário pediu para sair. Se a sessão já havia expirado, o aviso de expiração prevalece. */
  porLogout(): void {
    this.encerrar({});
  }

  private encerrar(queryParams: Params): void {
    if (this.encerrando) {
      return;
    }

    this.encerrando = true;
    this.sessao.limpar();

    // replaceUrl descarta a área autenticada do histórico: voltar não a reexibe.
    void this.router.navigate([ROTA_LOGIN], { queryParams, replaceUrl: true }).finally(() => {
      this.encerrando = false;
    });
  }
}
