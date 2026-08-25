import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { ErroApi } from '../../core/models/erro-api.model';
import { rotaInicial } from '../../core/navegacao/rota-inicial';
import { AutenticacaoService } from '../../core/services/autenticacao.service';
import { SessaoService } from '../../core/services/sessao.service';
import { CabecalhoComponent } from '../../shared/components/cabecalho/cabecalho.component';
import { MensagemErroComponent } from '../../shared/components/mensagem-erro/mensagem-erro.component';
import { RodapeComponent } from '../../shared/components/rodape/rodape.component';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    CabecalhoComponent,
    RodapeComponent,
    MensagemErroComponent,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required]],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly senhaVisivel = signal(false);

  protected alternarVisibilidadeDaSenha(): void {
    this.senhaVisivel.update((visivel) => !visivel);
  }

  protected entrar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.autenticacao.entrar(this.formulario.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl(rotaInicial(this.sessao.perfil())),
      error: (falha: ErroApi) => {
        this.erro.set(falha.message);
        this.enviando.set(false);
        this.formulario.controls.senha.reset();
      },
    });
  }
}
