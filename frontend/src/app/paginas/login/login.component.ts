import { Component, afterNextRender, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ErroApi } from '../../core/models/erro-api.model';
import { rotaInicial } from '../../core/navegacao/rota-inicial';
import { AutenticacaoService } from '../../core/services/autenticacao.service';
import { SessaoService } from '../../core/services/sessao.service';
import { CabecalhoComponent } from '../../shared/components/cabecalho/cabecalho.component';
import { MensagemAvisoComponent } from '../../shared/components/mensagem-aviso/mensagem-aviso.component';
import { MensagemErroComponent } from '../../shared/components/mensagem-erro/mensagem-erro.component';
import { RodapeComponent } from '../../shared/components/rodape/rodape.component';

const MENSAGEM_SESSAO_EXPIRADA =
  'Sua sessão expirou por inatividade. Entre novamente para continuar.';

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
    MensagemAvisoComponent,
    MensagemErroComponent,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly autenticacao = inject(AutenticacaoService);
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);
  private readonly rota = inject(ActivatedRoute);

  private readonly returnUrl = this.rota.snapshot.queryParamMap.get('returnUrl');

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required]],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly senhaVisivel = signal(false);

  constructor() {
    if (this.rota.snapshot.queryParamMap.get('motivo') === 'expirada') {
      this.aviso.set(MENSAGEM_SESSAO_EXPIRADA);
      afterNextRender(() => this.removerMotivoDaUrl());
    }
  }

  protected alternarVisibilidadeDaSenha(): void {
    this.senhaVisivel.update((visivel) => !visivel);
  }

  protected entrar(): void {
    if (this.enviando()) {
      return;
    }

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);
    this.aviso.set(null);

    this.autenticacao.entrar(this.formulario.getRawValue()).subscribe({
      next: () => this.irParaAreaDoUsuario(),
      error: (falha: ErroApi) => {
        // O e-mail digitado permanece no formulário; apenas a senha é descartada.
        this.erro.set(falha.message);
        this.enviando.set(false);
        this.formulario.controls.senha.reset();
      },
    });
  }

  private irParaAreaDoUsuario(): void {
    void this.router.navigateByUrl(this.destinoAposLogin(), { replaceUrl: true });
  }

  private destinoAposLogin(): string {
    const pretendida = this.returnUrl;
    const ehInterna =
      pretendida !== null && pretendida.startsWith('/') && !pretendida.startsWith('//');

    return ehInterna ? pretendida : rotaInicial(this.sessao.perfil());
  }

  private removerMotivoDaUrl(): void {
    void this.router.navigate([], {
      relativeTo: this.rota,
      queryParams: { motivo: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
