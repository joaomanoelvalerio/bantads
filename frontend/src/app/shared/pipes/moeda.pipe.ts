import { Pipe, PipeTransform } from '@angular/core';
import Decimal from 'decimal.js';

const VALOR_DECIMAL = /^-?\d+(\.\d+)?$/;
const MILHAR = /\B(?=(\d{3})+(?!\d))/g;

@Pipe({ name: 'moeda' })
export class MoedaPipe implements PipeTransform {
  transform(valor: string): string {
    const bruto = valor.trim();

    if (!VALOR_DECIMAL.test(bruto)) {
      return '';
    }

    const numero = new Decimal(bruto);
    const [inteiro, centavos] = numero.abs().toFixed(2).split('.');
    const sinal = numero.isNegative() ? '-' : '';

    return `${sinal}R$ ${inteiro.replace(MILHAR, '.')},${centavos}`;
  }
}
