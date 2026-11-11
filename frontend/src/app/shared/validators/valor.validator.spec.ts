import { FormControl } from '@angular/forms';
import { valorValidator } from './valor.validator';

/**
 * valorValidator reaproveita o parser de salario.validator.ts (já coberto em
 * detalhe em salario.validator.spec.ts) — aqui só confere o próprio contrato:
 * chaves de erro diferentes (valorFormato/valorPositivo), já que depósito/
 * saque/transferência não compartilham as chaves salarioFormato/salarioPositivo
 * do autocadastro.
 */
describe('valorValidator', () => {
  it('não reclama de um controle vazio', () => {
    const controle = new FormControl('');

    expect(valorValidator(controle)).toBeNull();
  });

  it('aceita um valor de operação positivo bem formatado', () => {
    const controle = new FormControl('150,00');

    expect(valorValidator(controle)).toBeNull();
  });

  it('devolve { valorFormato: true } para texto mal formatado — não { salarioFormato }', () => {
    const controle = new FormControl('abc');

    expect(valorValidator(controle)).toEqual({ valorFormato: true });
  });

  it('devolve { valorPositivo: true } para zero — não { salarioPositivo }', () => {
    const controle = new FormControl('0');

    expect(valorValidator(controle)).toEqual({ valorPositivo: true });
  });

  it('devolve { valorFormato: true } para valor negativo — "-" não faz parte do formato aceito', () => {
    const controle = new FormControl('-50,00');

    expect(valorValidator(controle)).toEqual({ valorFormato: true });
  });
});
