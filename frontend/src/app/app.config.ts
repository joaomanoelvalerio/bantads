import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { MAT_DATE_LOCALE, MatDateFormats } from '@angular/material/core';
import { provideLuxonDateAdapter } from '@angular/material-luxon-adapter';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { erroInterceptor } from './core/interceptors/erro.interceptor';
import { tokenInterceptor } from './core/interceptors/token.interceptor';
import { SessaoService } from './core/services/sessao.service';
import { routes } from './app.routes';

const DATE_FORMATS: MatDateFormats = {
  parse: {
    dateInput: ['dd/MM/yyyy', 'd/M/yyyy', 'dd/MM/yy', 'd/M/yy', 'ddMMyyyy'],
  },
  display: {
    dateInput: 'dd/MM/yyyy',
    monthYearLabel: 'LLL yyyy',
    dateA11yLabel: 'DDD',
    monthYearA11yLabel: 'LLLL yyyy',
  },
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Restaura a sessão persistida antes da primeira resolução de rota, para que um
    // reload em rota protegida não pisque a tela de login.
    provideAppInitializer(() => inject(SessaoService).restaurar()),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideHttpClient(withInterceptors([tokenInterceptor, erroInterceptor])),
    provideLuxonDateAdapter(DATE_FORMATS),
    { provide: MAT_DATE_LOCALE, useValue: 'pt-BR' },
  ],
};
