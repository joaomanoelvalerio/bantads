import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Conta } from '../models/conta.model';
import { Extrato } from '../models/movimentacao.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class ContaService {
  private readonly api = inject(ApiService);

  consultarPorCpf(cpf: string): Observable<Conta> {
    return this.api.get<Conta>(`/contas/${encodeURIComponent(cpf)}`);
  }

  /**
   * Depósito, saque e transferência não devolvem o novo saldo: o lado de consulta do
   * CQRS é atualizado por mensageria. Quem precisa do saldo reconsulta a conta.
   */
  depositar(numero: string, valor: string): Observable<void> {
    return this.api.post<void>(`${this.enderecoDaConta(numero)}/deposito`, { valor });
  }

  sacar(numero: string, valor: string): Observable<void> {
    return this.api.post<void>(`${this.enderecoDaConta(numero)}/saque`, { valor });
  }

  transferir(numero: string, contaDestino: string, valor: string): Observable<void> {
    return this.api.post<void>(`${this.enderecoDaConta(numero)}/transferencia`, {
      contaDestino,
      valor,
    });
  }

  /** Datas no formato ISO `yyyy-MM-dd`, já resolvidas no fuso de São Paulo. */
  consultarExtrato(numero: string, dataInicio: string, dataFim: string): Observable<Extrato> {
    const parametros = new URLSearchParams({ dataInicio, dataFim });

    return this.api.get<Extrato>(`${this.enderecoDaConta(numero)}/extrato?${parametros}`);
  }

  private enderecoDaConta(numero: string): string {
    return `/contas/${encodeURIComponent(numero)}`;
  }
}
