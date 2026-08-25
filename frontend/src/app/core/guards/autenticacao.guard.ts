import { inject } from '@angular/core';
import { CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { ROTA_LOGIN } from '../navegacao/rota-inicial';
import { SessaoService } from '../services/sessao.service';

export const autenticacaoGuard: CanActivateFn = (_rota, estado: RouterStateSnapshot) => {
  const sessao = inject(SessaoService);
  const router = inject(Router);

  if (sessao.estaAutenticado()) {
    return true;
  }

  return router.createUrlTree([ROTA_LOGIN], { queryParams: { returnUrl: estado.url } });
};
