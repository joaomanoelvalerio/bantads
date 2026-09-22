import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ClienteListado } from '../models/cliente.model';
import { NovoCliente, Solicitacao } from '../models/solicitacao.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly api = inject(ApiService);

  solicitarAbertura(cliente: NovoCliente): Observable<Solicitacao> {
    return this.api.post<Solicitacao>('/clientes', cliente);
  }

  /**
   * R11. Todos os clientes do banco, já ordenados por nome pelo back-end. O
   * contrato não prevê parâmetro de busca: a pesquisa é feita pela tela.
   */
  listar(): Observable<readonly ClienteListado[]> {
    return this.api.get<readonly ClienteListado[]>('/clientes');
  }
}
