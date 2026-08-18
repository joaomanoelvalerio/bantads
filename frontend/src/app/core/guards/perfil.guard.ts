import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { PerfilUsuario } from '../models/usuario.model';
import { SessaoService } from '../services/sessao.service';

export const perfilGuard: CanActivateFn = (rota: ActivatedRouteSnapshot) => {
  const sessao = inject(SessaoService);
  const router = inject(Router);
  const perfilExigido = rota.data['perfil'] as PerfilUsuario;

  return sessao.perfil() === perfilExigido ? true : router.createUrlTree([sessao.rotaInicial()]);
};
