import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Conta } from '../models/conta.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class ContaService {
  private readonly api = inject(ApiService);

  consultarPorCpf(cpf: string): Observable<Conta> {
    return this.api.get<Conta>(`/contas/${encodeURIComponent(cpf)}`);
  }
}
