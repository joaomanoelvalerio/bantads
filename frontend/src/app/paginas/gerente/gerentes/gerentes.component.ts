import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { ErroApi } from '../../../core/models/erro-api.model';
import { Gerente } from '../../../core/models/gerente.model';
import { GerenteService } from '../../../core/services/gerente.service';
import { MensagemErroComponent } from '../../../shared/components/mensagem-erro/mensagem-erro.component';
import { DocumentoPipe } from '../../../shared/pipes/documento.pipe';
import { confirmacaoRecebida } from './navegacao-gerentes';

/**
 * R12. A lista é consultada toda vez que a tela abre, então volta de R13 ou R14
 * já com o estado novo. A ordem é a do back-end.
 */
@Component({
  selector: 'app-gerentes',
  imports: [
    RouterLink,
    MatButtonModule,
    MatProgressSpinnerModule,
    MensagemErroComponent,
    DocumentoPipe,
  ],
  templateUrl: './gerentes.component.html',
  styleUrl: './gerentes.component.scss',
})
export class GerentesComponent {
  private readonly gerentes = inject(GerenteService);
  private readonly destruicao = inject(DestroyRef);

  protected readonly lista = signal<readonly Gerente[]>([]);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly confirmacao = signal<string | null>(confirmacaoRecebida(inject(Router)));

  constructor() {
    this.carregar();
  }

  protected carregar(): void {
    if (this.carregando()) {
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    this.gerentes
      .listar()
      .pipe(takeUntilDestroyed(this.destruicao))
      .subscribe({
        next: (gerentes) => {
          this.lista.set(gerentes);
          this.carregando.set(false);
        },
        error: (falha: ErroApi) => {
          this.carregando.set(false);

          // 401 já foi tratado pelo interceptor, que encerra a sessão e leva ao login.
          if (falha.status !== 401) {
            this.erro.set(falha.message);
          }
        },
      });
  }
}
