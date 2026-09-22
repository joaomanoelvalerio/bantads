import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { switchMap } from 'rxjs';
import { ErroApi } from '../../../core/models/erro-api.model';
import { DesfechoDoJob } from '../../../core/models/job.model';
import { LinhaDoRelatorio } from '../../../core/models/relatorio.model';
import { JobsService } from '../../../core/services/jobs.service';
import { RelatorioService } from '../../../core/services/relatorio.service';
import { MensagemAvisoComponent } from '../../../shared/components/mensagem-aviso/mensagem-aviso.component';
import { MensagemErroComponent } from '../../../shared/components/mensagem-erro/mensagem-erro.component';
import { DocumentoPipe } from '../../../shared/pipes/documento.pipe';
import { MoedaPipe } from '../../../shared/pipes/moeda.pipe';

type Situacao = 'GERANDO' | 'PRONTO' | 'FALHA' | 'TEMPO_ESGOTADO';

const FORMATO_INESPERADO = 'O relatório foi gerado em um formato que a tela não reconhece.';

/**
 * R16. Tela própria, separada de R11: as colunas e a origem do dado são outras.
 * A geração é um job com resultado inline; sair da tela encerra o polling.
 */
@Component({
  selector: 'app-relatorio-clientes',
  imports: [
    MatButtonModule,
    MatProgressSpinnerModule,
    MensagemAvisoComponent,
    MensagemErroComponent,
    DocumentoPipe,
    MoedaPipe,
  ],
  templateUrl: './relatorio-clientes.component.html',
  styleUrl: './relatorio-clientes.component.scss',
})
export class RelatorioClientesComponent {
  private readonly relatorios = inject(RelatorioService);
  private readonly jobs = inject(JobsService);
  private readonly destruicao = inject(DestroyRef);

  protected readonly situacao = signal<Situacao>('GERANDO');
  protected readonly linhas = signal<readonly LinhaDoRelatorio[]>([]);
  protected readonly mensagemDeFalha = signal<string | null>(null);

  constructor() {
    this.gerar();
  }

  protected gerar(): void {
    this.situacao.set('GERANDO');
    this.linhas.set([]);
    this.mensagemDeFalha.set(null);

    this.relatorios
      .solicitarRelatorioDeClientes()
      .pipe(
        // O jobId vem do corpo do 202 e de mais lugar nenhum.
        switchMap((aceite) =>
          this.jobs.acompanhar<readonly LinhaDoRelatorio[]>(aceite.jobId, this.destruicao),
        ),
        takeUntilDestroyed(this.destruicao),
      )
      .subscribe({
        next: (desfecho) => this.tratarDesfecho(desfecho),
        error: (falha: ErroApi) => {
          // 401 já foi tratado pelo interceptor, que encerra a sessão e leva ao login.
          if (falha.status !== 401) {
            this.falhar(falha.message);
          }
        },
      });
  }

  private tratarDesfecho(desfecho: DesfechoDoJob<readonly LinhaDoRelatorio[]>): void {
    switch (desfecho.tipo) {
      case 'CONCLUIDO':
        if (!Array.isArray(desfecho.resultado)) {
          this.falhar(FORMATO_INESPERADO);
          return;
        }

        this.linhas.set(desfecho.resultado);
        this.situacao.set('PRONTO');
        break;

      case 'FALHA':
        this.falhar(desfecho.mensagem);
        break;

      case 'TEMPO_ESGOTADO':
        this.situacao.set('TEMPO_ESGOTADO');
        break;
    }
  }

  private falhar(mensagem: string): void {
    this.mensagemDeFalha.set(mensagem);
    this.situacao.set('FALHA');
  }
}
