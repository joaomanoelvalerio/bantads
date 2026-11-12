import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ZipCodeAddress, ZipCodeService } from './zip-code.service';

describe('ZipCodeService', () => {
  let service: ZipCodeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ZipCodeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('maps the ViaCEP response to an address', () => {
    let result: ZipCodeAddress | null | undefined;
    service.lookup('80060000').subscribe((address) => (result = address));

    http.expectOne('https://viacep.com.br/ws/80060000/json/').flush({
      cep: '80060-000',
      logradouro: 'Rua XV de Novembro',
      bairro: 'Centro',
      localidade: 'Curitiba',
      uf: 'PR',
    });

    expect(result).toEqual({ street: 'Rua XV de Novembro', city: 'Curitiba', state: 'PR' });
  });

  it('emits null when the zip code does not exist', () => {
    let result: ZipCodeAddress | null | undefined;
    service.lookup('99999999').subscribe((address) => (result = address));

    http.expectOne('https://viacep.com.br/ws/99999999/json/').flush({ erro: 'true' });

    expect(result).toBeNull();
  });

  it('returns an empty street for a city-wide zip code', () => {
    let result: ZipCodeAddress | null | undefined;
    service.lookup('85960000').subscribe((address) => (result = address));

    http
      .expectOne('https://viacep.com.br/ws/85960000/json/')
      .flush({ cep: '85960-000', logradouro: '', localidade: 'Marechal Cândido Rondon', uf: 'PR' });

    expect(result).toEqual({ street: '', city: 'Marechal Cândido Rondon', state: 'PR' });
  });
});
