import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClienteListado } from '../../../core/models/cliente.model';
import { ErroApi } from '../../../core/models/erro-api.model';
import { ClienteService } from '../../../core/services/cliente.service';
import { MensagemErroComponent } from '../../../shared/components/mensagem-erro/mensagem-erro.component';
import { apenasDigitos } from '../../../shared/formato/mascara-valor';
import { DocumentoPipe } from '../../../shared/pipes/documento.pipe';
import { MoedaPipe } from '../../../shared/pipes/moeda.pipe';

/**
 * R11. O contrato de `GET /clientes` não prevê parâmetro de busca, então a
 * pesquisa filtra a lista já carregada: sem nova requisição, sem reordenar e sem
 * alterar a lista original.
 */
@Component({
  selector: 'app-clientes',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MensagemErroComponent,
    DocumentoPipe,
    MoedaPipe,
  ],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.scss',
})
export class ClientesComponent {
  private readonly clientes = inject(ClienteService);
  private readonly destruicao = inject(DestroyRef);

  protected readonly lista = signal<readonly ClienteListado[]>([]);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly termo = signal('');

  protected readonly linhas = computed<readonly ClienteListado[]>(() => {
    const termo = this.termo().trim();

    if (termo.length === 0) {
      return this.lista();
    }

    const nomeBuscado = semAcento(termo);
    const cpfBuscado = apenasDigitos(termo);

    return this.lista().filter(
      (cliente) =>
        semAcento(cliente.nome).includes(nomeBuscado) ||
        (cpfBuscado.length > 0 && cliente.cpf.includes(cpfBuscado)),
    );
  });

  constructor() {
    this.carregar();
  }

  protected carregar(): void {
    if (this.carregando()) {
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    this.clientes
      .listar()
      .pipe(takeUntilDestroyed(this.destruicao))
      .subscribe({
        next: (clientes) => {
          this.lista.set(clientes);
          this.carregando.set(false);
        },
        error: (falha: ErroApi) => {
          this.carregando.set(false);

          // 401 já foi tratado pelo interceptor, que encerra a sessão e leva ao login.
          if (falha.status !== 401) {
            this.erro.set(falha.message);
          }
        },
      });
  }

  protected pesquisar(evento: Event): void {
    this.termo.set((evento.target as HTMLInputElement).value);
  }

  protected limparBusca(): void {
    this.termo.set('');
  }
}

/** "Catharyna", "cathárina" e "CAT" se comparam pela mesma chave. */
function semAcento(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}
