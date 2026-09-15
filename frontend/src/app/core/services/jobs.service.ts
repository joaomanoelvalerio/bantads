import { DestroyRef, Injectable, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  Observable,
  catchError,
  concatMap,
  defer,
  first,
  map,
  of,
  switchMap,
  tap,
  throwError,
  timeout,
  timer,
} from 'rxjs';
import { DesfechoDoJob, StatusDoJob } from '../models/job.model';
import { ErroApi } from '../models/erro-api.model';
import { ApiService } from './api.service';

const INTERVALO_ENTRE_CONSULTAS_MS = 2_000;

/**
 * Teto do acompanhamento. O job expira no Redis em 5 minutos, mas esperar até lá
 * deixaria o gerente travado numa tela sem resposta; passado o limite o front para
 * de consultar e devolve TEMPO_ESGOTADO, sem afirmar sucesso nem falha.
 */
const LIMITE_TOTAL_MS = 90_000;

/** Uma consulta que falha não derruba o acompanhamento; três seguidas, sim. */
const FALHAS_DE_REDE_TOLERADAS = 3;

const FALHA_SEM_TEXTO = 'A operação falhou, mas o servidor não informou o motivo.';
const RESULTADO_INDEFINIDO = 'A operação foi concluída sem informar onde está o resultado.';

/**
 * Acompanhamento genérico de jobs assíncronos, sem nada de um caso de uso
 * específico: quem chama diz o jobId e o tipo do resultado esperado.
 */
@Injectable({ providedIn: 'root' })
export class JobsService {
  private readonly api = inject(ApiService);

  /**
   * Consulta o status do job a cada dois segundos até um estado final e resolve o
   * resultado conforme o `resultType`. Emite uma única vez e completa.
   *
   * O `destruicao` é obrigatório de propósito: o polling termina junto com quem o
   * iniciou, em vez de depender de o chamador lembrar de cancelar a inscrição.
   * Nenhuma consulta continua depois que a tela sai do ar.
   */
  acompanhar<T>(jobId: string, destruicao: DestroyRef): Observable<DesfechoDoJob<T>> {
    // defer para que o contador de falhas pertença a cada inscrição, não ao Observable.
    return defer(() => {
      let falhasSeguidas = 0;

      return timer(0, INTERVALO_ENTRE_CONSULTAS_MS).pipe(
        // concatMap serializa: uma consulta nunca começa antes de a anterior terminar,
        // então um back-end lento não acumula requisições em voo.
        concatMap(() =>
          this.consultarStatus(jobId).pipe(
            catchError((falha: ErroApi) => {
              falhasSeguidas += 1;

              // EMPTY descarta esta rodada e deixa o timer disparar a próxima.
              return falhasSeguidas > FALHAS_DE_REDE_TOLERADAS ? throwError(() => falha) : EMPTY;
            }),
          ),
        ),
        tap(() => {
          falhasSeguidas = 0;
        }),
        // Estado não final mantém o polling; o primeiro final encerra o timer.
        first(ehEstadoFinal),
        switchMap((estado) => this.resolver<T>(jobId, estado)),
        timeout({
          first: LIMITE_TOTAL_MS,
          with: () => of<DesfechoDoJob<T>>({ tipo: 'TEMPO_ESGOTADO' }),
        }),
        takeUntilDestroyed(destruicao),
      );
    });
  }

  private consultarStatus(jobId: string): Observable<StatusDoJob> {
    return this.api.get<StatusDoJob>(`/jobs/${encodeURIComponent(jobId)}/status`);
  }

  private resolver<T>(jobId: string, estado: StatusDoJob): Observable<DesfechoDoJob<T>> {
    if (estado.status === 'FALHA') {
      return of<DesfechoDoJob<T>>({ tipo: 'FALHA', mensagem: estado.erro ?? FALHA_SEM_TEXTO });
    }

    if (estado.resultType === 'resource') {
      if (estado.resourceId === null || estado.dominio.length === 0) {
        return of<DesfechoDoJob<T>>({ tipo: 'FALHA', mensagem: RESULTADO_INDEFINIDO });
      }

      return this.concluirCom(
        this.api.get<T>(`/${estado.dominio}/${encodeURIComponent(estado.resourceId)}`),
      );
    }

    if (estado.resultType === 'inline') {
      return this.concluirCom(this.api.get<T>(`/jobs/${encodeURIComponent(jobId)}/result`));
    }

    return of<DesfechoDoJob<T>>({ tipo: 'FALHA', mensagem: RESULTADO_INDEFINIDO });
  }

  private concluirCom<T>(busca: Observable<T>): Observable<DesfechoDoJob<T>> {
    return busca.pipe(map((resultado): DesfechoDoJob<T> => ({ tipo: 'CONCLUIDO', resultado })));
  }
}

function ehEstadoFinal(estado: StatusDoJob): boolean {
  return estado.status === 'CONCLUIDO' || estado.status === 'FALHA';
}
