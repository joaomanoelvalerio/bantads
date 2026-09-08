/** Descarta tudo o que não for dígito. */
export function apenasDigitos(valor: string): string {
  return valor.replace(/\D/g, '');
}

/**
 * Reescreve o que foi digitado no formato brasileiro, tratando os dígitos como
 * centavos: `1` vira `0,01` e `12345` vira `123,45`. Nunca produz number.
 */
export function mascaraValor(digitos: string): string {
  const limitado = digitos.replace(/^0+/, '').slice(0, 11);

  if (limitado.length === 0) {
    return '';
  }

  const centavos = limitado.padStart(3, '0');
  const inteiro = centavos.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, '.');

  return `${inteiro},${centavos.slice(-2)}`;
}

/** Número de conta é string de 4 dígitos e preserva zero à esquerda. */
export function mascaraNumeroDeConta(digitos: string): string {
  return digitos.slice(0, 4);
}
