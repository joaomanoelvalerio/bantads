import { DestroyRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DesfechoDoJob, StatusDoJob } from '../models/job.model';
import { ErroApi } from '../models/erro-api.model';
import { ApiService } from './api.service';
import { JobsService } from './jobs.service';

const JOB = 'job-1';

function pendente(): StatusDoJob {
  return {
    jobId: JOB,
    status: 'PENDENTE',
    resultType: null,
    dominio: 'clientes',
    resourceId: null,
    erro: null,
  };
}

/** ApiService de mentira: devolve, em ordem, uma resposta por chamada de GET. */
class ApiDeTeste {
  readonly caminhos: string[] = [];

  constructor(private readonly respostas: Observable<unknown>[]) {}

  get<T>(caminho: string): Observable<T> {
    this.caminhos.push(caminho);
    const proxima = this.respostas.shift();

    if (proxima === undefined) {
      throw new Error(`consulta inesperada a ${caminho}`);
    }

    return proxima as Observable<T>;
  }
}

/** DestroyRef de mentira, para disparar a destruição manualmente. */
class DestruicaoDeTeste {
  private readonly ouvintes: (() => void)[] = [];

  onDestroy(retorno: () => void): () => void {
    this.ouvintes.push(retorno);
    return () => undefined;
  }

  destruir(): void {
    for (const ouvinte of this.ouvintes) {
      ouvinte();
    }
  }
}

function montar(respostas: Observable<unknown>[]): {
  jobs: JobsService;
  api: ApiDeTeste;
  destruicao: DestruicaoDeTeste;
} {
  const api = new ApiDeTeste(respostas);
  TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });

  return { jobs: TestBed.inject(JobsService), api, destruicao: new DestruicaoDeTeste() };
}

function acompanhar<T>(
  jobs: JobsService,
  destruicao: DestruicaoDeTeste,
): { desfechos: DesfechoDoJob<T>[]; falhas: unknown[] } {
  const desfechos: DesfechoDoJob<T>[] = [];
  const falhas: unknown[] = [];

  jobs
    .acompanhar<T>(JOB, destruicao as unknown as DestroyRef)
    .subscribe({ next: (d) => desfechos.push(d), error: (f: unknown) => falhas.push(f) });

  return { desfechos, falhas };
}

describe('JobsService', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.resetTestingModule();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('consulta a cada dois segundos e resolve o recurso apontado ao concluir', () => {
    const cliente = { cpf: '12912861012', nome: 'Catharyna' };
    const { jobs, api, destruicao } = montar([
      of(pendente()),
      of<StatusDoJob>({
        jobId: JOB,
        status: 'CONCLUIDO',
        resultType: 'resource',
        dominio: 'clientes',
        resourceId: '12912861012',
        erro: null,
      }),
      of(cliente),
    ]);

    const { desfechos } = acompanhar<typeof cliente>(jobs, destruicao);

    vi.advanceTimersByTime(0);
    expect(api.caminhos).toEqual([`/jobs/${JOB}/status`]);

    vi.advanceTimersByTime(2000);

    expect(desfechos).toEqual([{ tipo: 'CONCLUIDO', resultado: cliente }]);
    expect(api.caminhos).toEqual([
      `/jobs/${JOB}/status`,
      `/jobs/${JOB}/status`,
      '/clientes/12912861012',
    ]);
  });

  it('busca o resultado embutido em /jobs/{id}/result', () => {
    const resultado = { total: '10.00' };
    const { jobs, api, destruicao } = montar([
      of<StatusDoJob>({
        jobId: JOB,
        status: 'CONCLUIDO',
        resultType: 'inline',
        dominio: 'clientes',
        resourceId: null,
        erro: null,
      }),
      of(resultado),
    ]);

    const { desfechos } = acompanhar<typeof resultado>(jobs, destruicao);

    vi.advanceTimersByTime(0);
    expect(desfechos).toEqual([{ tipo: 'CONCLUIDO', resultado }]);
    expect(api.caminhos).toContain(`/jobs/${JOB}/result`);
  });

  it('propaga a mensagem do campo erro em FALHA', () => {
    const { jobs, destruicao } = montar([
      of<StatusDoJob>({
        jobId: JOB,
        status: 'FALHA',
        resultType: null,
        dominio: 'clientes',
        resourceId: null,
        erro: 'Mensagem de Erro',
      }),
    ]);

    const { desfechos } = acompanhar(jobs, destruicao);

    vi.advanceTimersByTime(0);
    expect(desfechos).toEqual([{ tipo: 'FALHA', mensagem: 'Mensagem de Erro' }]);
  });

  it('tolera até três falhas de rede seguidas e desiste na quarta', () => {
    const rede = () => throwError(() => new ErroApi(0, 'Serviço indisponível'));
    const { jobs, destruicao } = montar([rede(), rede(), rede(), rede()]);

    const { desfechos, falhas } = acompanhar(jobs, destruicao);

    vi.advanceTimersByTime(6000);

    expect(desfechos).toEqual([]);
    expect(falhas).toHaveLength(1);
  });

  it('volta a contar as falhas do zero depois de uma consulta bem-sucedida', () => {
    const rede = () => throwError(() => new ErroApi(0, 'Serviço indisponível'));
    const { jobs, destruicao } = montar([rede(), rede(), rede(), of(pendente()), rede(), rede()]);

    const { falhas } = acompanhar(jobs, destruicao);

    vi.advanceTimersByTime(10_000);

    expect(falhas).toEqual([]);
  });

  it('encerra com tempo esgotado aos 90 segundos, sem continuar consultando', () => {
    const respostas = Array.from({ length: 60 }, () => of(pendente()));
    const { jobs, api, destruicao } = montar(respostas);

    const { desfechos } = acompanhar(jobs, destruicao);

    vi.advanceTimersByTime(90_000);

    expect(desfechos).toEqual([{ tipo: 'TEMPO_ESGOTADO' }]);

    const consultasAteOLimite = api.caminhos.length;
    vi.advanceTimersByTime(30_000);

    expect(api.caminhos).toHaveLength(consultasAteOLimite);
  });

  it('para de consultar quando quem iniciou o polling é destruído', () => {
    const respostas = Array.from({ length: 10 }, () => of(pendente()));
    const { jobs, api, destruicao } = montar(respostas);

    const { desfechos } = acompanhar(jobs, destruicao);

    vi.advanceTimersByTime(4000);
    const consultasAteAliParaDeCrescer = api.caminhos.length;

    destruicao.destruir();
    vi.advanceTimersByTime(20_000);

    expect(api.caminhos).toHaveLength(consultasAteAliParaDeCrescer);
    expect(desfechos).toEqual([]);
  });
});
