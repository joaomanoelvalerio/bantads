import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { rotaInicial } from '../navegacao/rota-inicial';
import { SessaoService } from '../services/sessao.service';

/** Impede que um usuário já autenticado volte para login ou autocadastro. */
export const visitanteGuard: CanActivateFn = () => {
  const sessao = inject(SessaoService);
  const router = inject(Router);

  if (!sessao.estaAutenticado()) {
    return true;
  }

  return router.createUrlTree([rotaInicial(sessao.perfil())]);
};
