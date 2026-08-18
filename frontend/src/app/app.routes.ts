import { Routes } from '@angular/router';
import { autenticacaoGuard } from './core/guards/autenticacao.guard';
import { perfilGuard } from './core/guards/perfil.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    title: 'BANTADS · Entrar',
    loadComponent: () =>
      import('./paginas/login/login.component').then((arquivo) => arquivo.LoginComponent),
  },
  {
    path: 'autocadastro',
    title: 'BANTADS · Abrir conta',
    loadComponent: () =>
      import('./paginas/autocadastro/autocadastro.component').then(
        (arquivo) => arquivo.AutocadastroComponent,
      ),
  },
  {
    path: '',
    canActivate: [autenticacaoGuard],
    loadComponent: () =>
      import('./shared/components/layout-autenticado/layout-autenticado.component').then(
        (arquivo) => arquivo.LayoutAutenticadoComponent,
      ),
    children: [
      {
        path: 'cliente',
        title: 'BANTADS · Cliente',
        canActivate: [perfilGuard],
        data: { perfil: 'CLIENTE' },
        loadComponent: () =>
          import('./paginas/cliente/cliente.component').then((arquivo) => arquivo.ClienteComponent),
      },
      {
        path: 'gerente',
        title: 'BANTADS · Gerente',
        canActivate: [perfilGuard],
        data: { perfil: 'GERENTE' },
        loadComponent: () =>
          import('./paginas/gerente/gerente.component').then((arquivo) => arquivo.GerenteComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
