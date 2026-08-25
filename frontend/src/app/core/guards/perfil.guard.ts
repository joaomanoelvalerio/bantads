import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { PerfilUsuario } from '../models/usuario.model';
import { rotaInicial } from '../navegacao/rota-inicial';
import { SessaoService } from '../services/sessao.service';

export const perfilGuard: CanActivateFn = (rota: ActivatedRouteSnapshot) => {
  const sessao = inject(SessaoService);
  const router = inject(Router);
  const perfilExigido = rota.data['perfil'] as PerfilUsuario;

  if (sessao.perfil() === perfilExigido) {
    return true;
  }

  // Perfil errado devolve o usuário para a própria área, não para o login.
  return router.createUrlTree([rotaInicial(sessao.perfil())]);
};
