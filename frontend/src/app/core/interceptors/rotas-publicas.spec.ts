import { HttpRequest } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ehRotaPublica } from './rotas-publicas';

describe('ehRotaPublica', () => {
  it('reconhece POST /login como pública', () => {
    const requisicao = new HttpRequest('POST', `${environment.apiUrl}/login`, {});

    expect(ehRotaPublica(requisicao)).toBe(true);
  });

  it('reconhece POST /solicitacoes (autocadastro) como pública', () => {
    const requisicao = new HttpRequest('POST', `${environment.apiUrl}/solicitacoes`, {});

    expect(ehRotaPublica(requisicao)).toBe(true);
  });

  it('ignora query string ao comparar o caminho', () => {
    const requisicao = new HttpRequest(
      'POST',
      `${environment.apiUrl}/solicitacoes?origem=teste`,
      {},
    );

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

  it('não reconhece rotas fora da lista, como /solicitacoes/{cpf}/aprovacao', () => {
    const requisicao = new HttpRequest(
      'POST',
      `${environment.apiUrl}/solicitacoes/123/aprovacao`,
      {},
    );

    expect(ehRotaPublica(requisicao)).toBe(false);
  });

  it('é sensível ao método, não só ao caminho — PUT /solicitacoes não é público', () => {
    const requisicao = new HttpRequest('PUT', `${environment.apiUrl}/solicitacoes`, {});

    expect(ehRotaPublica(requisicao)).toBe(false);
  });
});
