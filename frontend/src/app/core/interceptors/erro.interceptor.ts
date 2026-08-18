import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ErroApi } from '../models/erro-api.model';
import { SessaoService } from '../services/sessao.service';

const CAMPOS_DE_MENSAGEM = ['message', 'mensagem', 'error', 'erro'];
const MENSAGEM_INDISPONIVEL =
  'Serviço indisponível no momento. Verifique sua conexão e tente novamente.';
const MENSAGEM_GENERICA = 'Não foi possível concluir a operação. Tente novamente.';

export const erroInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const sessao = inject(SessaoService);
  const router = inject(Router);

  return proximo(requisicao).pipe(
    catchError((resposta: HttpErrorResponse) => {
      const sessaoExpirada = resposta.status === 401 && !requisicao.url.endsWith('/login');

      if (sessaoExpirada) {
        sessao.limpar();
        router.navigate(['/login']);
      }

      return throwError(() => new ErroApi(resposta.status, extrairMensagem(resposta)));
    }),
  );
};

function extrairMensagem(resposta: HttpErrorResponse): string {
  if (resposta.status === 0) {
    return MENSAGEM_INDISPONIVEL;
  }

  const corpo: unknown = resposta.error;

  if (typeof corpo === 'string' && corpo.trim().length > 0) {
    return corpo;
  }

  if (corpo !== null && typeof corpo === 'object') {
    for (const campo of CAMPOS_DE_MENSAGEM) {
      const valor = (corpo as Record<string, unknown>)[campo];
      if (typeof valor === 'string' && valor.trim().length > 0) {
        return valor;
      }
    }
  }

  return MENSAGEM_GENERICA;
}
