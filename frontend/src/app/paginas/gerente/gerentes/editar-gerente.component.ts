import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ErroApi } from '../../../core/models/erro-api.model';
import { AlteracaoDeGerente, Gerente } from '../../../core/models/gerente.model';
import { GerenteService } from '../../../core/services/gerente.service';
import { MensagemErroComponent } from '../../../shared/components/mensagem-erro/mensagem-erro.component';
import { mascaraTelefone } from '../../../shared/formato/mascara-documento';
import { apenasDigitos } from '../../../shared/formato/mascara-valor';
import { DocumentoPipe } from '../../../shared/pipes/documento.pipe';
import { ROTA_LISTAGEM_DE_GERENTES, estadoComConfirmacao } from './navegacao-gerentes';

/**
 * R14. Operação síncrona, sem job. CPF e e-mail não são alteráveis e por isso
 * ficam fora do formulário: são exibidos a partir do gerente carregado e nunca
 * passam pelo cpfValidator — o seed tem um CPF que não fecha os dígitos
 * verificadores e travaria o formulário inteiro.
 */
@Component({
  selector: 'app-editar-gerente',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MensagemErroComponent,
    DocumentoPipe,
  ],
  templateUrl: './editar-gerente.component.html',
  styleUrl: './editar-gerente.component.scss',
})
export class EditarGerenteComponent {
  private readonly gerentes = inject(GerenteService);
  private readonly router = inject(Router);
  private readonly destruicao = inject(DestroyRef);
  private readonly cpf = inject(ActivatedRoute).snapshot.paramMap.get('cpf') ?? '';

  protected readonly rotaDaListagem = ROTA_LISTAGEM_DE_GERENTES;

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    telefone: ['', [Validators.required, Validators.pattern(/^\(\d{2}\) \d{4,5}-\d{4}$/)]],
  });

  protected readonly gerente = signal<Gerente | null>(null);
  protected readonly carregando = signal(false);
  protected readonly naoEncontrado = signal(false);
  protected readonly erroDeCarga = signal<string | null>(null);

  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  constructor() {
    this.carregar();
  }

  protected carregar(): void {
    if (this.carregando()) {
      return;
    }

    this.carregando.set(true);
    this.naoEncontrado.set(false);
    this.erroDeCarga.set(null);

    this.gerentes
      .consultar(this.cpf)
      .pipe(takeUntilDestroyed(this.destruicao))
      .subscribe({
        next: (gerente) => {
          this.gerente.set(gerente);
          this.formulario.setValue({
            nome: gerente.nome,
            telefone: mascaraTelefone(apenasDigitos(gerente.telefone)),
          });
          this.carregando.set(false);
        },
        error: (falha: ErroApi) => {
          this.carregando.set(false);

          if (falha.status === 404) {
            this.naoEncontrado.set(true);
          } else if (falha.status !== 401) {
            this.erroDeCarga.set(falha.message);
          }
        },
      });
  }

  protected mascararTelefone(): void {
    const controle = this.formulario.controls.telefone;
    controle.setValue(mascaraTelefone(apenasDigitos(controle.value)));
  }

  protected salvar(): void {
    const gerente = this.gerente();

    if (gerente === null || this.salvando()) {
      return;
    }

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const valores = this.formulario.getRawValue();
    const alteracao: AlteracaoDeGerente = {
      nome: valores.nome.trim(),
      telefone: apenasDigitos(valores.telefone),
    };

    this.salvando.set(true);
    this.erro.set(null);

    // O CPF do endereço é o do gerente carregado, não um valor editável.
    this.gerentes
      .atualizar(gerente.cpf, alteracao)
      .pipe(takeUntilDestroyed(this.destruicao))
      .subscribe({
        next: () => {
          this.salvando.set(false);
          void this.router.navigate([ROTA_LISTAGEM_DE_GERENTES], {
            state: estadoComConfirmacao(`Dados de ${alteracao.nome} atualizados.`),
          });
        },
        error: (falha: ErroApi) => {
          // O que foi digitado permanece no formulário.
          this.salvando.set(false);

          if (falha.status !== 401) {
            this.erro.set(falha.message);
          }
        },
      });
  }
}
