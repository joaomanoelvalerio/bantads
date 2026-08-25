import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ErroApi } from '../models/erro-api.model';
import { EncerramentoSessaoService } from '../services/encerramento-sessao.service';
import { ehRotaPublica } from './rotas-publicas';

const CAMPOS_DE_MENSAGEM = ['message', 'mensagem', 'error', 'erro'];
const MENSAGEM_INDISPONIVEL =
  'Serviço indisponível no momento. Verifique sua conexão e tente novamente.';
const MENSAGEM_GENERICA = 'Não foi possível concluir a operação. Tente novamente.';

export const erroInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const encerramento = inject(EncerramentoSessaoService);

  return proximo(requisicao).pipe(
    catchError((resposta: HttpErrorResponse) => {
      // 401 em rota pública é credencial recusada e pertence à própria tela.
      // Em rota protegida significa que o Gateway não reconhece mais a sessão.
      if (resposta.status === 401 && !ehRotaPublica(requisicao)) {
        encerramento.porExpiracao();
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
