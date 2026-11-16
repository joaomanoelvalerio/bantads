import { MoedaPipe } from './moeda.pipe';

describe('MoedaPipe', () => {
  let pipe: MoedaPipe;

  beforeEach(() => {
    pipe = new MoedaPipe();
  });

  it.each([
    ['10000', 'R$ 10.000,00'],
    ['800', 'R$ 800,00'],
    ['1234567.89', 'R$ 1.234.567,89'],
    ['0', 'R$ 0,00'],
  ])('formata "%s" como "%s"', (entrada, esperado) => {
    expect(pipe.transform(entrada)).toBe(esperado);
  });

  it('coloca o sinal antes de "R$" para valores negativos', () => {
    expect(pipe.transform('-50.5')).toBe('-R$ 50,50');
  });

  it('arredonda pra duas casas decimais (meio pra cima)', () => {
    expect(pipe.transform('100.005')).toBe('R$ 100,01');
  });

  it('aceita espaços nas pontas', () => {
    expect(pipe.transform(' 100 ')).toBe('R$ 100,00');
  });

  it('devolve string vazia para entrada não numérica', () => {
    expect(pipe.transform('abc')).toBe('');
  });

  it('devolve string vazia para entrada vazia', () => {
    expect(pipe.transform('')).toBe('');
  });

  it('devolve string vazia para formato com vírgula em vez de ponto — este pipe espera o formato de API, não o brasileiro', () => {
    expect(pipe.transform('100,00')).toBe('');
  });
});
