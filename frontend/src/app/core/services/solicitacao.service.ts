import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { RespostaAceita } from '../models/job.model';
import {
  SolicitacaoAnalisada,
  SolicitacaoDto,
  normalizarSolicitacao,
} from '../models/solicitacao.model';
import { ApiService } from './api.service';

/** Solicitações de autocadastro sob o ponto de vista do gerente (R8 e R9). */
@Injectable({ providedIn: 'root' })
export class SolicitacaoService {
  private readonly api = inject(ApiService);

  /** Lista as solicitações em todos os estados: Pendente, Aprovado e Não aprovado. */
  listar(): Observable<readonly SolicitacaoAnalisada[]> {
    return this.api
      .get<readonly SolicitacaoDto[]>('/solicitacoes')
      .pipe(map((solicitacoes) => solicitacoes.map(normalizarSolicitacao)));
  }

  /**
   * R9. Dispara a SAGA de aprovação, que responde 202 com o jobId. O resultado
   * não vem daqui: quem acompanha é o JobsService.
   */
  aprovar(cpf: string): Observable<RespostaAceita> {
    return this.api.post<RespostaAceita>(`/solicitacoes/${encodeURIComponent(cpf)}/aprovar`, {});
  }
}
