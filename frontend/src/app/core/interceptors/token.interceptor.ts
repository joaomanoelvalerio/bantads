import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { SessaoService } from '../services/sessao.service';
import { ehRotaPublica } from './rotas-publicas';

export const tokenInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const token = inject(SessaoService).token();

  const isGatewayRequest = requisicao.url.startsWith(environment.apiUrl);

  if (token === null || !isGatewayRequest || ehRotaPublica(requisicao)) {
    return proximo(requisicao);
  }

  return proximo(requisicao.clone({ setHeaders: { 'x-access-token': token } }));
};
