import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { AutenticacaoService } from '../../../core/services/autenticacao.service';
import { EncerramentoSessaoService } from '../../../core/services/encerramento-sessao.service';
import { SessaoService } from '../../../core/services/sessao.service';

@Component({
  selector: 'app-cabecalho',
  imports: [MatButtonModule],
  templateUrl: './cabecalho.component.html',
  styleUrl: './cabecalho.component.scss',
})
export class CabecalhoComponent {
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly encerramento = inject(EncerramentoSessaoService);

  protected readonly sessao = inject(SessaoService);
  protected readonly saindo = signal(false);

  protected readonly rotuloDoPerfil = computed(() => {
    switch (this.sessao.perfil()) {
      case 'CLIENTE':
        return 'Cliente';
      case 'GERENTE':
        return 'Gerente';
      default:
        return '';
    }
  });

  protected sair(): void {
    if (this.saindo()) {
      return;
    }

    this.saindo.set(true);

    // A sessão local é encerrada mesmo que o Gateway recuse ou não responda.
    this.autenticacao.sair().subscribe({
      next: () => this.concluirSaida(),
      error: () => this.concluirSaida(),
    });
  }

  private concluirSaida(): void {
    this.saindo.set(false);
    this.encerramento.porLogout();
  }
}
