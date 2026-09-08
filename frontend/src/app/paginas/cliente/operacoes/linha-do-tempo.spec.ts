import { describe, expect, it } from 'vitest';
import { Extrato } from '../../../core/models/movimentacao.model';
import { diaDeIso, montarLinhaDoTempo, sentidoDe } from './linha-do-tempo';

const extrato: Extrato = {
  saldoAnterior: '800.0000',
  movimentacoes: [
    {
      dataHora: '2020-01-20T12:00:00',
      tipo: 'TRANSFERENCIA',
      cpfOrigem: '12912861012',
      nomeOrigem: 'Catharyna',
      cpfDestino: '09506382000',
      nomeDestino: 'Cleuddonio',
      valor: '1700.0000',
    },
  ],
};

describe('linha do tempo do extrato', () => {
  const inicio = diaDeIso('2020-01-01');
  const fim = diaDeIso('2020-01-31');

  it('cobre todos os dias do periodo, inclusive os sem movimentacao', () => {
    const dias = montarLinhaDoTempo(extrato, '12912861012', inicio, fim);
    expect(dias).toHaveLength(31);
    expect(dias[0].chave).toBe('2020-01-01');
    expect(dias[30].chave).toBe('2020-01-31');
    expect(dias[0].lancamentos).toHaveLength(0);
    expect(dias[0].saldoConsolidado).toBe('800.00');
    expect(dias[18].saldoConsolidado).toBe('800.00');
  });

  it('debita a transferencia para a origem e repete o saldo depois', () => {
    const dias = montarLinhaDoTempo(extrato, '12912861012', inicio, fim);
    const vinte = dias[19];
    expect(vinte.chave).toBe('2020-01-20');
    expect(vinte.lancamentos[0].sentido).toBe('SAIDA');
    expect(vinte.saldoConsolidado).toBe('-900.00');
    expect(dias[30].saldoConsolidado).toBe('-900.00');
  });

  it('credita a mesma transferencia para o destino', () => {
    const dias = montarLinhaDoTempo(extrato, '09506382000', inicio, fim);
    expect(dias[19].lancamentos[0].sentido).toBe('ENTRADA');
    expect(dias[19].saldoConsolidado).toBe('2500.00');
  });

  it('agrupa pelo fuso de Sao Paulo, nao pelo do navegador', () => {
    const noturno: Extrato = {
      saldoAnterior: '0.0000',
      movimentacoes: [{ dataHora: '2020-01-11T02:30:00Z', tipo: 'DEPOSITO', valor: '50.0000' }],
    };
    const dias = montarLinhaDoTempo(
      noturno,
      '12912861012',
      diaDeIso('2020-01-10'),
      diaDeIso('2020-01-11'),
    );
    expect(dias[0].lancamentos).toHaveLength(1);
    expect(dias[0].saldoConsolidado).toBe('50.00');
    expect(dias[1].lancamentos).toHaveLength(0);
  });

  it('classifica saque como saida e deposito como entrada', () => {
    expect(sentidoDe({ dataHora: '', tipo: 'SAQUE', valor: '1' }, '12912861012')).toBe('SAIDA');
    expect(sentidoDe({ dataHora: '', tipo: 'DEPOSITO', valor: '1' }, '12912861012')).toBe(
      'ENTRADA',
    );
  });
});
