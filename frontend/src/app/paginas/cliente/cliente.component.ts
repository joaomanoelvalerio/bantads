import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Conta } from '../../core/models/conta.model';
import { ErroApi } from '../../core/models/erro-api.model';
import { ContaService } from '../../core/services/conta.service';
import { SessaoService } from '../../core/services/sessao.service';
import { MensagemErroComponent } from '../../shared/components/mensagem-erro/mensagem-erro.component';
import { MoedaPipe } from '../../shared/pipes/moeda.pipe';
import { MENU_CLIENTE } from './menu-cliente';

@Component({
  selector: 'app-cliente',
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatButtonModule,
    MatProgressSpinnerModule,
    MensagemErroComponent,
    MoedaPipe,
  ],
  templateUrl: './cliente.component.html',
  styleUrl: './cliente.component.scss',
})
export class ClienteComponent {
  private readonly contas = inject(ContaService);
  private readonly sessao = inject(SessaoService);

  protected readonly itens = MENU_CLIENTE;
  protected readonly usuario = this.sessao.usuario;

  protected readonly conta = signal<Conta | null>(null);
  protected readonly consultando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly saldoNegativo = computed(
    () => this.conta()?.saldo.trim().startsWith('-') ?? false,
  );

  protected readonly rotuloDaAtualizacao = computed(() => {
    if (this.consultando()) {
      return 'Atualizando...';
    }

    return this.erro() === null ? 'Atualizar saldo' : 'Tentar novamente';
  });

  constructor() {
    this.consultar();
  }

  protected consultar(): void {
    const cpf = this.usuario()?.cpf;

    if (cpf === undefined || this.consultando()) {
      return;
    }

    this.consultando.set(true);
    this.erro.set(null);

    this.contas.consultarPorCpf(cpf).subscribe({
      next: (conta) => {
        this.conta.set(conta);
        this.consultando.set(false);
      },
      error: (falha: ErroApi) => {
        this.consultando.set(false);

        if (falha.status !== 401) {
          this.erro.set(falha.message);
        }
      },
    });
  }
}
