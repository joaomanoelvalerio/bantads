import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { NovoCliente, Solicitacao } from '../models/solicitacao.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly api = inject(ApiService);

  solicitarAbertura(cliente: NovoCliente): Observable<Solicitacao> {
    return this.api.post<Solicitacao>('/clientes', cliente);
  }
}
