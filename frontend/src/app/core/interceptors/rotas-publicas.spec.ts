import { HttpRequest } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ehRotaPublica } from './rotas-publicas';

describe('ehRotaPublica', () => {
  it('reconhece POST /login como pública', () => {
    const requisicao = new HttpRequest('POST', `${environment.apiUrl}/login`, {});

    expect(ehRotaPublica(requisicao)).toBe(true);
  });

  it('reconhece POST /clientes (autocadastro) como pública', () => {
    const requisicao = new HttpRequest('POST', `${environment.apiUrl}/clientes`, {});

    expect(ehRotaPublica(requisicao)).toBe(true);
  });

  it('ignora query string ao comparar o caminho', () => {
    const requisicao = new HttpRequest('POST', `${environment.apiUrl}/clientes?origem=teste`, {});

    expect(ehRotaPublica(requisicao)).toBe(true);
  });

  it('funciona também com URL relativa, sem o prefixo apiUrl', () => {
    const requisicao = new HttpRequest('POST', '/login', {});

    expect(ehRotaPublica(requisicao)).toBe(true);
  });

  it('não reconhece GET /login — só o POST é público', () => {
    const requisicao = new HttpRequest('GET', `${environment.apiUrl}/login`);

    expect(ehRotaPublica(requisicao)).toBe(false);
  });

  it('não reconhece GET /clientes — bare listing é protegido (R11)', () => {
    const requisicao = new HttpRequest('GET', `${environment.apiUrl}/clientes`);

    expect(ehRotaPublica(requisicao)).toBe(false);
  });

  it('não reconhece rotas fora da lista, como /solicitacoes', () => {
    const requisicao = new HttpRequest(
      'POST',
      `${environment.apiUrl}/solicitacoes/123/aprovar`,
      {},
    );

    expect(ehRotaPublica(requisicao)).toBe(false);
  });

  it('é sensível ao método, não só ao caminho — PUT /clientes não é público', () => {
    const requisicao = new HttpRequest('PUT', `${environment.apiUrl}/clientes`, {});

    expect(ehRotaPublica(requisicao)).toBe(false);
  });
});
