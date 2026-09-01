import { Routes } from '@angular/router';
import { autenticacaoGuard } from './core/guards/autenticacao.guard';
import { perfilGuard } from './core/guards/perfil.guard';
import { visitanteGuard } from './core/guards/visitante.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    title: 'BANTADS · Entrar',
    canActivate: [visitanteGuard],
    loadComponent: () =>
      import('./paginas/login/login.component').then((arquivo) => arquivo.LoginComponent),
  },
  {
    path: 'autocadastro',
    title: 'BANTADS · Abrir conta',
    canActivate: [visitanteGuard],
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
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./paginas/cliente/operacoes/inicio.component').then(
                (arquivo) => arquivo.InicioClienteComponent,
              ),
          },
          {
            path: 'deposito',
            title: 'BANTADS · Depósito',
            loadComponent: () =>
              import('./paginas/cliente/operacoes/deposito.component').then(
                (arquivo) => arquivo.DepositoComponent,
              ),
          },
          {
            path: 'saque',
            title: 'BANTADS · Saque',
            loadComponent: () =>
              import('./paginas/cliente/operacoes/saque.component').then(
                (arquivo) => arquivo.SaqueComponent,
              ),
          },
          {
            path: 'transferencia',
            title: 'BANTADS · Transferência',
            loadComponent: () =>
              import('./paginas/cliente/operacoes/transferencia.component').then(
                (arquivo) => arquivo.TransferenciaComponent,
              ),
          },
          {
            path: 'extrato',
            title: 'BANTADS · Extrato',
            loadComponent: () =>
              import('./paginas/cliente/operacoes/extrato.component').then(
                (arquivo) => arquivo.ExtratoComponent,
              ),
          },
        ],
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
