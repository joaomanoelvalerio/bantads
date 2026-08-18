import { Pipe, PipeTransform } from '@angular/core';

export type TipoDocumento = 'cpf' | 'telefone';

@Pipe({ name: 'documento' })
export class DocumentoPipe implements PipeTransform {
  transform(valor: string, tipo: TipoDocumento): string {
    const digitos = valor.replace(/\D/g, '');

    if (tipo === 'cpf') {
      return formatarCpf(digitos, valor);
    }

    return formatarTelefone(digitos, valor);
  }
}

function formatarCpf(digitos: string, original: string): string {
  if (digitos.length !== 11) {
    return original;
  }

  return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`;
}

function formatarTelefone(digitos: string, original: string): string {
  if (digitos.length === 11) {
    return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 7)}-${digitos.slice(7)}`;
  }

  if (digitos.length === 10) {
    return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 6)}-${digitos.slice(6)}`;
  }

  return original;
}
