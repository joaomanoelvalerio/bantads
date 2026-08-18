import { AbstractControl, ValidationErrors } from '@angular/forms';
import Decimal from 'decimal.js';

export function paraDecimal(valorBrasileiro: string): Decimal | null {
  const bruto = valorBrasileiro.trim();

  if (!/^\d{1,3}(\.\d{3})*(,\d{1,2})?$|^\d+(,\d{1,2})?$/.test(bruto)) {
    return null;
  }

  return new Decimal(bruto.replace(/\./g, '').replace(',', '.'));
}

export function salarioValidator(controle: AbstractControl): ValidationErrors | null {
  const valor = typeof controle.value === 'string' ? controle.value : '';

  if (valor.trim().length === 0) {
    return null;
  }

  const convertido = paraDecimal(valor);

  if (convertido === null) {
    return { salarioFormato: true };
  }

  return convertido.greaterThan(0) ? null : { salarioPositivo: true };
}
