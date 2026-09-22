/** Reescreve os dígitos no formato `000.000.000-00`, à medida que são digitados. */
export function mascaraCpf(digitos: string): string {
  const limitado = digitos.slice(0, 11);
  const partes = [limitado.slice(0, 3), limitado.slice(3, 6), limitado.slice(6, 9)].filter(
    (parte) => parte.length > 0,
  );
  const verificador = limitado.slice(9);

  return partes.join('.') + (verificador.length > 0 ? `-${verificador}` : '');
}

/** Reescreve os dígitos como `(00) 0000-0000` ou `(00) 00000-0000`. */
export function mascaraTelefone(digitos: string): string {
  const limitado = digitos.slice(0, 11);

  if (limitado.length <= 2) {
    return limitado;
  }

  const corte = limitado.length > 10 ? 7 : 6;
  const prefixo = `(${limitado.slice(0, 2)}) ${limitado.slice(2, corte)}`;
  const sufixo = limitado.slice(corte);

  return sufixo.length > 0 ? `${prefixo}-${sufixo}` : prefixo;
}
