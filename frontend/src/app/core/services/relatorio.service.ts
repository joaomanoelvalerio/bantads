import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RespostaAceita } from '../models/job.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class RelatorioService {
  private readonly api = inject(ApiService);

  /**
   * R16. A geração é assíncrona: responde 202 com o jobId e o relatório chega
   * inline pelo JobsService.
   */
  solicitarRelatorioDeClientes(): Observable<RespostaAceita> {
    return this.api.get<RespostaAceita>('/relatorios/clientes');
  }
}
