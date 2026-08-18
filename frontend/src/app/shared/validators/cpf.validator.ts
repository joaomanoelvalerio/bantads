import { AbstractControl, ValidationErrors } from '@angular/forms';

export function cpfValidator(controle: AbstractControl): ValidationErrors | null {
  const valor = typeof controle.value === 'string' ? controle.value : '';
  const digitos = valor.replace(/\D/g, '');

  if (digitos.length === 0) {
    return null;
  }

  return cpfEhValido(digitos) ? null : { cpf: true };
}

export function cpfEhValido(digitos: string): boolean {
  if (digitos.length !== 11 || /^(\d)\1{10}$/.test(digitos)) {
    return false;
  }

  return (
    digitoVerificador(digitos, 9) === Number(digitos[9]) &&
    digitoVerificador(digitos, 10) === Number(digitos[10])
  );
}

function digitoVerificador(digitos: string, tamanho: number): number {
  let soma = 0;

  for (let posicao = 0; posicao < tamanho; posicao += 1) {
    soma += Number(digitos[posicao]) * (tamanho + 1 - posicao);
  }

  const resto = (soma * 10) % 11;

  return resto === 10 ? 0 : resto;
}
