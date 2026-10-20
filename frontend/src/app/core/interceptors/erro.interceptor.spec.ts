import {
  HttpErrorResponse,
  HttpHandlerFn,
  HttpRequest,
  HttpStatusCode,
} from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ErroApi } from '../models/erro-api.model';
import { EncerramentoSessaoService } from '../services/encerramento-sessao.service';
import { erroInterceptor } from './erro.interceptor';

describe('erroInterceptor', () => {
  let encerramento: { porExpiracao: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    encerramento = { porExpiracao: vi.fn() };

    TestBed.configureTestingModule({
      providers: [{ provide: EncerramentoSessaoService, useValue: encerramento }],
    });
  });

  function rodar(
    resposta: HttpErrorResponse,
    url = `${environment.apiUrl}/contas/1291/saque`,
    metodo = 'POST',
  ): Promise<ErroApi> {
    const requisicao = new HttpRequest(metodo, url, {});
    const proximo: HttpHandlerFn = () => throwError(() => resposta);

    return new Promise((resolve, reject) => {
      TestBed.runInInjectionContext(() => erroInterceptor(requisicao, proximo)).subscribe({
        next: () => reject(new Error('esperava que o interceptor lançasse um erro')),
        error: (erro: ErroApi) => resolve(erro),
      });
    });
  }

  it('status 0 (sem conexão) vira a mensagem fixa de serviço indisponível', async () => {
    const resposta = new HttpErrorResponse({ status: 0, error: 'qualquer coisa' });

    const erro = await rodar(resposta);

    expect(erro).toBeInstanceOf(ErroApi);
    expect(erro.status).toBe(0);
    expect(erro.message).toBe(
      'Serviço indisponível no momento. Verifique sua conexão e tente novamente.',
    );
  });

  it('status 500 nunca repassa o texto da exceção, mesmo quando o corpo tem uma mensagem', async () => {
    const resposta = new HttpErrorResponse({
      status: 500,
      error: { message: 'NullPointerException em algum lugar' },
    });

    const erro = await rodar(resposta);

    expect(erro.status).toBe(500);
    expect(erro.message).toBe('Não foi possível concluir a operação. Tente novamente.');
  });

  it('extrai a mensagem quando o corpo é a própria string de erro', async () => {
    const resposta = new HttpErrorResponse({ status: 422, error: 'Saldo insuficiente' });

    const erro = await rodar(resposta);

    expect(erro.status).toBe(422);
    expect(erro.message).toBe('Saldo insuficiente');
  });

  it('extrai "message" quando o corpo é um objeto', async () => {
    const resposta = new HttpErrorResponse({
      status: 422,
      error: { message: 'Saldo insuficiente' },
    });

    const erro = await rodar(resposta);

    expect(erro.message).toBe('Saldo insuficiente');
  });

  it.each(['mensagem', 'error', 'erro'])(
    'também reconhece o campo "%s" quando "message" não existe',
    async (campo) => {
      const resposta = new HttpErrorResponse({ status: 409, error: { [campo]: 'Texto do campo' } });

      const erro = await rodar(resposta);

      expect(erro.message).toBe('Texto do campo');
    },
  );

  it('prioriza "message" sobre os demais campos quando mais de um está presente', async () => {
    const resposta = new HttpErrorResponse({
      status: 409,
      error: { message: 'Certo', mensagem: 'Errado', error: 'Errado também' },
    });

    const erro = await rodar(resposta);

    expect(erro.message).toBe('Certo');
  });

  it('cai na mensagem genérica quando o corpo é um objeto sem nenhum campo reconhecido', async () => {
    const resposta = new HttpErrorResponse({ status: 409, error: { timestamp: '2026-01-01' } });

    const erro = await rodar(resposta);

    expect(erro.message).toBe('Não foi possível concluir a operação. Tente novamente.');
  });

  it('cai na mensagem genérica quando o corpo é null', async () => {
    const resposta = new HttpErrorResponse({ status: 409, error: null });

    const erro = await rodar(resposta);

    expect(erro.message).toBe('Não foi possível concluir a operação. Tente novamente.');
  });

  it('cai na mensagem genérica quando o campo reconhecido está vazio ou só com espaços', async () => {
    const resposta = new HttpErrorResponse({ status: 409, error: { message: '   ' } });

    const erro = await rodar(resposta);

    expect(erro.message).toBe('Não foi possível concluir a operação. Tente novamente.');
  });

  it('401 numa rota protegida encerra a sessão por expiração', async () => {
    const resposta = new HttpErrorResponse({ status: HttpStatusCode.Unauthorized, error: {} });

    await rodar(resposta, `${environment.apiUrl}/contas/1291`, 'GET');

    expect(encerramento.porExpiracao).toHaveBeenCalledOnce();
  });

  it('401 numa rota pública (ex.: login com credencial errada) não encerra sessão nenhuma', async () => {
    const resposta = new HttpErrorResponse({
      status: HttpStatusCode.Unauthorized,
      error: { message: 'Login inválido!' },
    });

    const erro = await rodar(resposta, `${environment.apiUrl}/login`, 'POST');

    expect(encerramento.porExpiracao).not.toHaveBeenCalled();
    expect(erro.message).toBe('Login inválido!');
  });
});
