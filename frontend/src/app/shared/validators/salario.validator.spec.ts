import { FormControl } from '@angular/forms';
import { paraDecimal, salarioValidator } from './salario.validator';

describe('paraDecimal', () => {
  it.each([
    ['10.000,00', '10000'],
    ['10000,00', '10000'],
    ['1234', '1234'],
    ['0,50', '0.5'],
    ['1.234.567,89', '1234567.89'],
    ['10,5', '10.5'],
  ])('converte "%s" em Decimal(%s)', (entrada, esperado) => {
    expect(paraDecimal(entrada)?.toString()).toBe(esperado);
  });

  it('aceita espaços nas pontas (o campo já chega aparado, mas a função é defensiva)', () => {
    expect(paraDecimal('  100,00  ')?.toString()).toBe('100');
  });

  it('rejeita string vazia', () => {
    expect(paraDecimal('')).toBeNull();
  });

  it('rejeita texto não numérico', () => {
    expect(paraDecimal('abc')).toBeNull();
  });

  it('rejeita mais de duas casas decimais', () => {
    expect(paraDecimal('10,555')).toBeNull();
  });

  it('rejeita agrupamento de milhar incorreto (grupo com menos de 3 dígitos após o ponto)', () => {
    expect(paraDecimal('1.23,00')).toBeNull();
  });

  it('rejeita sinal negativo — o formato brasileiro aceito aqui não inclui "-"', () => {
    expect(paraDecimal('-10,00')).toBeNull();
  });
});

describe('salarioValidator', () => {
  it('não reclama de um controle vazio — "required" é responsabilidade de outro validador', () => {
    const controle = new FormControl('');

    expect(salarioValidator(controle)).toBeNull();
  });

  it('aceita um salário positivo bem formatado', () => {
    const controle = new FormControl('3.500,00');

    expect(salarioValidator(controle)).toBeNull();
  });

  it('devolve { salarioFormato: true } para texto mal formatado', () => {
    const controle = new FormControl('três mil');

    expect(salarioValidator(controle)).toEqual({ salarioFormato: true });
  });

  it('devolve { salarioPositivo: true } para zero', () => {
    const controle = new FormControl('0,00');

    expect(salarioValidator(controle)).toEqual({ salarioPositivo: true });
  });

  it('trata valor não-string (ex.: null) como vazio, sem lançar erro', () => {
    const controle = new FormControl<string | null>(null);

    expect(salarioValidator(controle)).toBeNull();
  });
});
