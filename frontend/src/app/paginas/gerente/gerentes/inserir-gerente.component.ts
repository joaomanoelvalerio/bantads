import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { ErroApi } from '../../../core/models/erro-api.model';
import { Gerente, NovoGerente } from '../../../core/models/gerente.model';
import { DesfechoDoJob } from '../../../core/models/job.model';
import { GerenteService } from '../../../core/services/gerente.service';
import { JobsService } from '../../../core/services/jobs.service';
import { MensagemAvisoComponent } from '../../../shared/components/mensagem-aviso/mensagem-aviso.component';
import { MensagemErroComponent } from '../../../shared/components/mensagem-erro/mensagem-erro.component';
import { mascaraCpf, mascaraTelefone } from '../../../shared/formato/mascara-documento';
import { apenasDigitos } from '../../../shared/formato/mascara-valor';
import { cpfValidator } from '../../../shared/validators/cpf.validator';
import { ROTA_LISTAGEM_DE_GERENTES } from './navegacao-gerentes';

/**
 * `PROCESSANDO` cobre o POST e o polling. `TEMPO_ESGOTADO` é o job que o front
 * deixou de consultar sem saber o desfecho: o formulário continua bloqueado para
 * não criar o mesmo gerente duas vezes.
 */
type Etapa = 'PREENCHENDO' | 'PROCESSANDO' | 'CONCLUIDO' | 'TEMPO_ESGOTADO';

/**
 * R13. A inserção é uma SAGA: o POST responde 202 e o desfecho vem pelo
 * JobsService. A tela não afirma nada sobre transferência de contas, que é
 * decisão do back-end.
 */
@Component({
  selector: 'app-inserir-gerente',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MensagemAvisoComponent,
    MensagemErroComponent,
  ],
  templateUrl: './inserir-gerente.component.html',
  styleUrl: './inserir-gerente.component.scss',
})
export class InserirGerenteComponent {
  private readonly gerentes = inject(GerenteService);
  private readonly jobs = inject(JobsService);
  private readonly destruicao = inject(DestroyRef);

  protected readonly rotaDaListagem = ROTA_LISTAGEM_DE_GERENTES;

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    cpf: ['', [Validators.required, cpfValidator]],
    telefone: ['', [Validators.required, Validators.pattern(/^\(\d{2}\) \d{4,5}-\d{4}$/)]],
    senha: ['', [Validators.required]],
    confirmacaoSenha: ['', [Validators.required, senhaConfirmada]],
  });

  protected readonly etapa = signal<Etapa>('PREENCHENDO');
  protected readonly erro = signal<string | null>(null);
  protected readonly nomeDoCriado = signal<string | null>(null);

  protected mascararCpf(): void {
    const controle = this.formulario.controls.cpf;
    controle.setValue(mascaraCpf(apenasDigitos(controle.value)));
  }

  protected mascararTelefone(): void {
    const controle = this.formulario.controls.telefone;
    controle.setValue(mascaraTelefone(apenasDigitos(controle.value)));
  }

  /** A confirmação depende da senha: revalida quando a senha muda. */
  protected revalidarConfirmacao(): void {
    this.formulario.controls.confirmacaoSenha.updateValueAndValidity();
  }

  protected enviar(): void {
    if (this.etapa() !== 'PREENCHENDO') {
      return;
    }

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const dados = this.montarPayload();

    // A senha só existe no corpo desta requisição: sai dos campos assim que é enviada.
    this.limparSenhas();
    this.etapa.set('PROCESSANDO');
    this.erro.set(null);
    this.formulario.disable();

    this.gerentes
      .inserir(dados)
      .pipe(
        // O jobId vem do corpo do 202 e de mais lugar nenhum.
        switchMap((aceite) => this.jobs.acompanhar<Gerente>(aceite.jobId, this.destruicao)),
        takeUntilDestroyed(this.destruicao),
      )
      .subscribe({
        next: (desfecho) => this.tratarDesfecho(desfecho),
        error: (falha: ErroApi) => {
          this.reabrir();

          // 401 já foi tratado pelo interceptor, que encerra a sessão e leva ao login.
          if (falha.status !== 401) {
            this.erro.set(falha.message);
          }
        },
      });
  }

  private tratarDesfecho(desfecho: DesfechoDoJob<Gerente>): void {
    switch (desfecho.tipo) {
      case 'CONCLUIDO':
        this.nomeDoCriado.set(desfecho.resultado.nome);
        this.formulario.reset();
        this.etapa.set('CONCLUIDO');
        break;

      case 'FALHA':
        // A SAGA compensa o que já tinha feito: o gerente pode não existir. O
        // formulário volta como estava, sem as senhas, para corrigir e reenviar.
        this.reabrir();
        this.erro.set(desfecho.mensagem);
        break;

      case 'TEMPO_ESGOTADO':
        // O job segue no back-end; o front só parou de consultar.
        this.etapa.set('TEMPO_ESGOTADO');
        break;
    }
  }

  private montarPayload(): NovoGerente {
    const valores = this.formulario.getRawValue();

    return {
      nome: valores.nome.trim(),
      email: valores.email.trim(),
      cpf: apenasDigitos(valores.cpf),
      telefone: apenasDigitos(valores.telefone),
      senha: valores.senha,
    };
  }

  private reabrir(): void {
    this.formulario.enable();
    this.limparSenhas();
    this.etapa.set('PREENCHENDO');
  }

  private limparSenhas(): void {
    const { senha, confirmacaoSenha } = this.formulario.controls;

    senha.reset();
    confirmacaoSenha.reset();
  }
}

function senhaConfirmada(controle: AbstractControl): ValidationErrors | null {
  const senha: unknown = controle.parent?.get('senha')?.value;

  if (controle.value === '' || controle.value === senha) {
    return null;
  }

  return { senhaDivergente: true };
}
