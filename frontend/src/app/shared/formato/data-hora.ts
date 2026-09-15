import { DateTime } from 'luxon';

/**
 * Fuso único em que o banco apresenta e agrupa datas e horas. Nunca o do
 * navegador: uma movimentação no fim da noite cairia no dia seguinte, o saldo
 * consolidado do dia sairia errado e uma decisão registrada perto da meia-noite
 * apareceria na data errada.
 */
export const FUSO_BANTADS = 'America/Sao_Paulo';

/** Converte um instante ISO vindo da API em `dd/MM/aaaa HH:mm`. */
export function dataHoraLegivel(iso: string): string | null {
  const momento = DateTime.fromISO(iso, { zone: FUSO_BANTADS });

  return momento.isValid ? momento.toFormat('dd/LL/yyyy HH:mm') : null;
}
