import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { AutenticacaoService } from '../../../core/services/autenticacao.service';
import { SessaoService } from '../../../core/services/sessao.service';
import { DocumentoPipe } from '../../pipes/documento.pipe';

@Component({
  selector: 'app-cabecalho',
  imports: [MatButtonModule, DocumentoPipe],
  templateUrl: './cabecalho.component.html',
  styleUrl: './cabecalho.component.scss',
})
export class CabecalhoComponent {
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly router = inject(Router);

  protected readonly sessao = inject(SessaoService);
  protected readonly saindo = signal(false);

  protected sair(): void {
    this.saindo.set(true);
    this.autenticacao.sair().subscribe({
      next: () => this.voltarParaLogin(),
      error: () => this.voltarParaLogin(),
    });
  }

  private voltarParaLogin(): void {
    this.saindo.set(false);
    this.router.navigate(['/login']);
  }
}
