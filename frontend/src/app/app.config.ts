import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { erroInterceptor } from './core/interceptors/erro.interceptor';
import { tokenInterceptor } from './core/interceptors/token.interceptor';
import { SessaoService } from './core/services/sessao.service';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Restaura a sessão persistida antes da primeira resolução de rota, para que um
    // reload em rota protegida não pisque a tela de login.
    provideAppInitializer(() => inject(SessaoService).restaurar()),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideHttpClient(withInterceptors([tokenInterceptor, erroInterceptor])),
  ],
};
