import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ClienteListado } from '../models/cliente.model';
import { NovoCliente, Solicitacao } from '../models/solicitacao.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly api = inject(ApiService);

  /** R1. A API espera o endereço aninhado em `endereco`, não solto no corpo. */
  solicitarAbertura(cliente: NovoCliente): Observable<Solicitacao> {
    const { cep, logradouro, numero, complemento, cidade, uf, ...dados } = cliente;
    const corpo = { ...dados, endereco: { cep, logradouro, numero, complemento, cidade, uf } };
    return this.api.post<Solicitacao>('/solicitacoes', corpo);
  }

  /**
   * R11. Todos os clientes do banco, já ordenados por nome pelo back-end. O
   * back-end também aceita `busca` (CPF ou nome), mas a tela já filtra ela
   * mesma sobre a lista completa — ver `clientes.component.ts`.
   */
  listar(): Observable<readonly ClienteListado[]> {
    return this.api
      .get<{ clientes: readonly ClienteListado[] }>('/clientes')
      .pipe(map((resposta) => resposta.clientes));
  }
}
