import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AlteracaoDeGerente, Gerente, NovoGerente } from '../models/gerente.model';
import { RespostaAceita } from '../models/job.model';
import { ApiService } from './api.service';

/** Cadastro de gerentes: listagem (R12), inserção (R13) e atualização (R14). */
@Injectable({ providedIn: 'root' })
export class GerenteService {
  private readonly api = inject(ApiService);

  /** Gerentes ativos, já ordenados por nome pelo back-end. */
  listar(): Observable<readonly Gerente[]> {
    return this.api.get<readonly Gerente[]>('/gerentes');
  }

  consultar(cpf: string): Observable<Gerente> {
    return this.api.get<Gerente>(this.enderecoDoGerente(cpf));
  }

  /**
   * R13. Dispara a SAGA de inserção, que responde 202 com o jobId. O resultado
   * não vem daqui: quem acompanha é o JobsService.
   */
  inserir(gerente: NovoGerente): Observable<RespostaAceita> {
    return this.api.post<RespostaAceita>('/gerentes', gerente);
  }

  /** R14. Operação síncrona: 200 com o gerente atualizado. */
  atualizar(cpf: string, alteracao: AlteracaoDeGerente): Observable<Gerente> {
    return this.api.put<Gerente>(this.enderecoDoGerente(cpf), alteracao);
  }

  private enderecoDoGerente(cpf: string): string {
    return `/gerentes/${encodeURIComponent(cpf)}`;
  }
}
