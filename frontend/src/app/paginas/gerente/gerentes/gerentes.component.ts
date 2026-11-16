import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { ErroApi } from '../../../core/models/erro-api.model';
import { Gerente } from '../../../core/models/gerente.model';
import { DesfechoDoJob } from '../../../core/models/job.model';
import { GerenteService } from '../../../core/services/gerente.service';
import { JobsService } from '../../../core/services/jobs.service';
import { SessaoService } from '../../../core/services/sessao.service';
import { MensagemAvisoComponent } from '../../../shared/components/mensagem-aviso/mensagem-aviso.component';
import { MensagemErroComponent } from '../../../shared/components/mensagem-erro/mensagem-erro.component';
import { apenasDigitos } from '../../../shared/formato/mascara-valor';
import { DocumentoPipe } from '../../../shared/pipes/documento.pipe';
import { ConfirmarRemocaoDialogComponent, DadosDaRemocao } from './confirmar-remocao.dialog';
import { confirmacaoRecebida } from './navegacao-gerentes';

/**
 * Linha em que a tela iniciou uma remoção. `REMOVENDO` cobre o DELETE e o
 * polling; `TEMPO_ESGOTADO` é o job que o front deixou de consultar sem saber o
 * desfecho. Nos dois casos as ações da linha ficam bloqueadas.
 */
type AndamentoDaLinha = 'REMOVENDO' | 'TEMPO_ESGOTADO';

const MENSAGEM_AUTORREMOCAO = 'Você não pode remover o seu próprio cadastro de gerente.';

/** Chaves em que o resultado inline pode trazer o texto, se não vier como string. */
const CAMPOS_DE_MENSAGEM = ['mensagem', 'message'];

/**
 * R12 e R15. A lista é consultada toda vez que a tela abre, então volta de R13 ou
 * R14 já com o estado novo. A ordem é a do back-end. Depois de uma remoção quem
 * manda é a lista recarregada, nunca uma suposição do front sobre a SAGA.
 */
@Component({
  selector: 'app-gerentes',
  imports: [
    RouterLink,
    MatButtonModule,
    MatProgressSpinnerModule,
    MensagemAvisoComponent,
    MensagemErroComponent,
    DocumentoPipe,
  ],
  templateUrl: './gerentes.component.html',
  styleUrl: './gerentes.component.scss',
})
export class GerentesComponent {
  private readonly gerentes = inject(GerenteService);
  private readonly jobs = inject(JobsService);
  private readonly dialogo = inject(MatDialog);
  private readonly sessao = inject(SessaoService);
  private readonly destruicao = inject(DestroyRef);

  protected readonly lista = signal<readonly Gerente[]>([]);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly confirmacao = signal<string | null>(confirmacaoRecebida(inject(Router)));

  protected readonly andamento = signal<ReadonlyMap<string, AndamentoDaLinha>>(new Map());
  protected readonly aviso = signal<string | null>(null);
  protected readonly erroDaAcao = signal<string | null>(null);

  /** CPF do gerente logado, só dígitos, para bloquear a remoção de si mesmo. */
  private readonly cpfDaSessao = computed(() => apenasDigitos(this.sessao.usuario()?.cpf ?? ''));

  protected readonly sessaoNaLista = computed(() =>
    this.lista().some((gerente) => this.ehOProprio(gerente)),
  );

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

  /** Recarrega a partir do aviso de tempo esgotado. */
  protected recarregarAposAviso(): void {
    this.aviso.set(null);
    this.carregar();
  }

  protected andamentoDe(cpf: string): AndamentoDaLinha | null {
    return this.andamento().get(cpf) ?? null;
  }

  protected ehOProprio(gerente: Gerente): boolean {
    const cpf = this.cpfDaSessao();

    return cpf.length > 0 && apenasDigitos(gerente.cpf) === cpf;
  }

  protected remover(gerente: Gerente): void {
    // O último gerente ativo não é checado aqui: a lista pode estar desatualizada
    // e a decisão é do back-end, que responde com a mensagem.
    if (
      this.ehOProprio(gerente) ||
      this.andamentoDe(gerente.cpf) !== null ||
      this.dialogo.openDialogs.length > 0
    ) {
      return;
    }

    this.dialogo
      .open<ConfirmarRemocaoDialogComponent, DadosDaRemocao, boolean>(
        ConfirmarRemocaoDialogComponent,
        {
          data: { nome: gerente.nome, cpf: gerente.cpf },
          width: '480px',
          maxWidth: 'calc(100vw - 32px)',
        },
      )
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destruicao))
      .subscribe((confirmado) => {
        if (confirmado === true) {
          this.iniciarRemocao(gerente);
        }
      });
  }

  private iniciarRemocao(gerente: Gerente): void {
    if (this.andamentoDe(gerente.cpf) !== null) {
      return;
    }

    this.marcar(gerente.cpf, 'REMOVENDO');
    this.confirmacao.set(null);
    this.aviso.set(null);
    this.erroDaAcao.set(null);

    this.gerentes
      .remover(gerente.cpf)
      .pipe(
        // O jobId vem do corpo do 202 e de mais lugar nenhum.
        switchMap((aceite) => this.jobs.acompanhar<unknown>(aceite.jobId, this.destruicao)),
        takeUntilDestroyed(this.destruicao),
      )
      .subscribe({
        next: (desfecho) => this.tratarDesfecho(gerente, desfecho),
        error: (falha: ErroApi) => {
          this.desmarcar(gerente.cpf);

          if (falha.status === 401) {
            return;
          }

          // 403 é a autorremoção recusada pelo Gateway antes da SAGA. O resto,
          // inclusive a regra do último gerente ativo, vem com a mensagem do back-end.
          this.erroDaAcao.set(falha.status === 403 ? MENSAGEM_AUTORREMOCAO : falha.message);
          this.carregar();
        },
      });
  }

  private tratarDesfecho(gerente: Gerente, desfecho: DesfechoDoJob<unknown>): void {
    switch (desfecho.tipo) {
      case 'CONCLUIDO':
        // A mensagem é a do back-end: o front não diz para onde as contas foram.
        this.desmarcar(gerente.cpf);
        this.confirmacao.set(
          mensagemDoResultado(desfecho.resultado) ?? `Remoção de ${gerente.nome} concluída.`,
        );
        this.carregar();
        break;

      case 'FALHA':
        // A SAGA compensa: o gerente pode ter voltado a Ativo. Vale a lista recarregada.
        this.desmarcar(gerente.cpf);
        this.erroDaAcao.set(desfecho.mensagem);
        this.carregar();
        break;

      case 'TEMPO_ESGOTADO':
        // O job segue no back-end; o front só parou de consultar. Nem sucesso nem falha.
        this.marcar(gerente.cpf, 'TEMPO_ESGOTADO');
        this.aviso.set(
          `A remoção de ${gerente.nome} ainda está em andamento. Recarregue a listagem em instantes para ver o resultado.`,
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
   * A lista recarregada já traz o estado real das linhas cujo job o front deixou
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

/** O resultado inline é uma mensagem: texto puro ou objeto com o texto num campo. */
function mensagemDoResultado(resultado: unknown): string | null {
  if (typeof resultado === 'string') {
    return resultado.trim().length > 0 ? resultado : null;
  }

  if (resultado !== null && typeof resultado === 'object') {
    for (const campo of CAMPOS_DE_MENSAGEM) {
      const valor = (resultado as Record<string, unknown>)[campo];

      if (typeof valor === 'string' && valor.trim().length > 0) {
        return valor;
      }
    }
  }

  return null;
}
