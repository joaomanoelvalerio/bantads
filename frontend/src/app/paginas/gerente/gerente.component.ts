import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { switchMap } from 'rxjs';
import { Cliente } from '../../core/models/cliente.model';
import { ErroApi } from '../../core/models/erro-api.model';
import { DesfechoDoJob } from '../../core/models/job.model';
import { SolicitacaoAnalisada, StatusSolicitacao } from '../../core/models/solicitacao.model';
import { JobsService } from '../../core/services/jobs.service';
import { SessaoService } from '../../core/services/sessao.service';
import { SolicitacaoService } from '../../core/services/solicitacao.service';
import { MensagemAvisoComponent } from '../../shared/components/mensagem-aviso/mensagem-aviso.component';
import { MensagemErroComponent } from '../../shared/components/mensagem-erro/mensagem-erro.component';
import { dataHoraLegivel } from '../../shared/formato/data-hora';
import { DocumentoPipe } from '../../shared/pipes/documento.pipe';
import { MoedaPipe } from '../../shared/pipes/moeda.pipe';

type FiltroDeStatus = 'TODAS' | StatusSolicitacao;

/**
 * Linha em que a tela iniciou uma aprovação. `ACOMPANHANDO` é o polling vivo;
 * `TEMPO_ESGOTADO` é o job que o front deixou de consultar sem saber o desfecho —
 * nos dois casos a linha fica sem botões, para não aprovar a mesma solicitação
 * duas vezes.
 */
type AndamentoDaLinha = 'ACOMPANHANDO' | 'TEMPO_ESGOTADO';

interface LinhaDaTabela {
  solicitacao: SolicitacaoAnalisada;
  rotuloDoStatus: string;
  analisadaEm: string | null;
}

const ROTULO_DO_STATUS: Readonly<Record<StatusSolicitacao, string>> = {
  PENDENTE: 'Pendente',
  APROVADO: 'Aprovado',
  NAO_APROVADO: 'Não aprovado',
};

const STATUS_DESCONHECIDO = 'Status não reconhecido';

const FILTROS: readonly { valor: FiltroDeStatus; rotulo: string }[] = [
  { valor: 'TODAS', rotulo: 'Todas' },
  { valor: 'PENDENTE', rotulo: 'Pendentes' },
  { valor: 'APROVADO', rotulo: 'Aprovadas' },
  { valor: 'NAO_APROVADO', rotulo: 'Não aprovadas' },
];

/**
 * R8 e R9. A tabela nunca se atualiza sozinha: quem manda é o dado recarregado,
 * por ação explícita ou depois de um job chegar a um desfecho.
 */
@Component({
  selector: 'app-gerente',
  imports: [
    MatButtonModule,
    MatButtonToggleModule,
    MatProgressSpinnerModule,
    MensagemAvisoComponent,
    MensagemErroComponent,
    DocumentoPipe,
    MoedaPipe,
  ],
  templateUrl: './gerente.component.html',
  styleUrl: './gerente.component.scss',
})
export class GerenteComponent {
  private readonly solicitacoes = inject(SolicitacaoService);
  private readonly jobs = inject(JobsService);
  private readonly destruicao = inject(DestroyRef);

  protected readonly sessao = inject(SessaoService);
  protected readonly filtros = FILTROS;

  protected readonly lista = signal<readonly SolicitacaoAnalisada[]>([]);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly filtro = signal<FiltroDeStatus>('TODAS');
  protected readonly andamento = signal<ReadonlyMap<string, AndamentoDaLinha>>(new Map());

  protected readonly sucesso = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly erroDaAcao = signal<string | null>(null);

  protected readonly linhas = computed<readonly LinhaDaTabela[]>(() => {
    const filtro = this.filtro();

    return this.lista()
      .filter((solicitacao) => filtro === 'TODAS' || solicitacao.status === filtro)
      .map(paraLinha);
  });

  constructor() {
    this.carregar();
  }

  protected carregar(): void {
    if (this.carregando()) {
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    this.solicitacoes
      .listar()
      .pipe(takeUntilDestroyed(this.destruicao))
      .subscribe({
        next: (solicitacoes) => {
          this.lista.set(solicitacoes);
          this.carregando.set(false);
          this.esquecerJobsSemDesfecho();
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

  protected aplicarFiltro(valor: FiltroDeStatus): void {
    this.filtro.set(valor);
  }

  protected andamentoDe(cpf: string): AndamentoDaLinha | null {
    return this.andamento().get(cpf) ?? null;
  }

  protected aprovar(solicitacao: SolicitacaoAnalisada): void {
    if (solicitacao.status !== 'PENDENTE' || this.andamentoDe(solicitacao.cpf) !== null) {
      return;
    }

    this.marcar(solicitacao.cpf, 'ACOMPANHANDO');
    this.sucesso.set(null);
    this.aviso.set(null);
    this.erroDaAcao.set(null);

    this.solicitacoes
      .aprovar(solicitacao.cpf)
      .pipe(
        // O jobId vem do corpo do 202 e de mais lugar nenhum.
        switchMap((aceite) => this.jobs.acompanhar<Cliente>(aceite.jobId, this.destruicao)),
        takeUntilDestroyed(this.destruicao),
      )
      .subscribe({
        next: (desfecho) => this.tratarDesfecho(solicitacao, desfecho),
        error: (falha: ErroApi) => {
          this.desmarcar(solicitacao.cpf);

          if (falha.status === 401) {
            return;
          }

          this.erroDaAcao.set(falha.message);
          this.carregar();
        },
      });
  }

  private tratarDesfecho(
    solicitacao: SolicitacaoAnalisada,
    desfecho: DesfechoDoJob<Cliente>,
  ): void {
    switch (desfecho.tipo) {
      case 'CONCLUIDO':
        this.desmarcar(solicitacao.cpf);
        this.sucesso.set(
          `Conta criada para ${desfecho.resultado.nome}. A senha de acesso foi enviada para ${desfecho.resultado.email}.`,
        );
        this.carregar();
        break;

      case 'FALHA':
        // A SAGA executa compensações: a solicitação pode ter voltado para Pendente
        // ou ido para Não aprovada. O front não supõe nada, recarrega.
        this.desmarcar(solicitacao.cpf);
        this.erroDaAcao.set(desfecho.mensagem);
        this.carregar();
        break;

      case 'TEMPO_ESGOTADO':
        // O job segue no back-end; o front só parou de consultar. Nem sucesso nem
        // falha, e a linha continua bloqueada até a tabela ser recarregada.
        this.marcar(solicitacao.cpf, 'TEMPO_ESGOTADO');
        this.aviso.set(
          `A aprovação de ${solicitacao.nome} ainda está em andamento. Recarregue a tabela em instantes para ver o resultado.`,
        );
        break;
    }
  }

  private marcar(cpf: string, estado: AndamentoDaLinha): void {
    this.andamento.update((atual) => new Map(atual).set(cpf, estado));
  }

  private desmarcar(cpf: string): void {
    this.andamento.update((atual) => {
      const proximo = new Map(atual);
      proximo.delete(cpf);

      return proximo;
    });
  }

  /**
   * A tabela recarregada já traz o estado real das linhas cujo job o front deixou
   * de acompanhar. Os pollings ainda vivos permanecem marcados.
   */
  private esquecerJobsSemDesfecho(): void {
    this.andamento.update((atual) => {
      const proximo = new Map(atual);

      for (const [cpf, estado] of atual) {
        if (estado === 'TEMPO_ESGOTADO') {
          proximo.delete(cpf);
        }
      }

      return proximo;
    });
  }
}

function paraLinha(solicitacao: SolicitacaoAnalisada): LinhaDaTabela {
  return {
    solicitacao,
    rotuloDoStatus:
      solicitacao.status === null ? STATUS_DESCONHECIDO : ROTULO_DO_STATUS[solicitacao.status],
    analisadaEm:
      solicitacao.dataHoraAnalise === null ? null : dataHoraLegivel(solicitacao.dataHoraAnalise),
  };
}
