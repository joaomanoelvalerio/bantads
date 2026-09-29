import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AlteracaoDeGerente, Gerente, NovoGerente } from '../models/gerente.model';
import { RespostaAceita } from '../models/job.model';
import { ApiService } from './api.service';

/** Cadastro de gerentes: listagem (R12), inserção (R13), atualização (R14) e remoção (R15). */
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

  /**
   * R15. Dispara a SAGA de remoção, que responde 202 com o jobId; o Gateway
   * recusa com 403 a remoção de si mesmo. A transferência das contas e a regra
   * do último gerente ativo são do back-end.
   */
  remover(cpf: string): Observable<RespostaAceita> {
    return this.api.delete<RespostaAceita>(this.enderecoDoGerente(cpf));
  }

  private enderecoDoGerente(cpf: string): string {
    return `/gerentes/${encodeURIComponent(cpf)}`;
  }
}
