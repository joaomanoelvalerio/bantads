import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SessaoService } from '../services/sessao.service';

export const tokenInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const token = inject(SessaoService).token();

  if (token === null) {
    return proximo(requisicao);
  }

  return proximo(requisicao.clone({ setHeaders: { 'x-access-token': token } }));
};
