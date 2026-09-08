import { AbstractControl, ValidationErrors } from '@angular/forms';
import { paraDecimal } from './salario.validator';

/**
 * Valor de operação monetária. Reaproveita o parser brasileiro do autocadastro, que
 * já recusa formato inválido e mais de duas casas decimais; aqui só resta barrar
 * zero e negativo. A validação definitiva continua sendo do back-end.
 */
export function valorValidator(controle: AbstractControl): ValidationErrors | null {
  const valor = typeof controle.value === 'string' ? controle.value : '';

  if (valor.trim().length === 0) {
    return null;
  }

  const convertido = paraDecimal(valor);

  if (convertido === null) {
    return { valorFormato: true };
  }

  return convertido.greaterThan(0) ? null : { valorPositivo: true };
}
