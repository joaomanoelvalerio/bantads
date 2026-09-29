import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MensagemErroComponent } from '../../shared/components/mensagem-erro/mensagem-erro.component';
import { DocumentoPipe } from '../../shared/pipes/documento.pipe';

/** Evita motivos de um caractere, que não explicam nada ao cliente. */
export const MOTIVO_MINIMO = 10;

/** Limite da coluna `motivo VARCHAR(200)` no schema do ms-cliente. */
export const MOTIVO_MAXIMO = 200;

export interface DadosDaRecusa {
  nome: string;
  cpf: string;
  /** Motivo de uma tentativa anterior que falhou, para permitir o reenvio. */
  motivo: string;
  /** Mensagem do back-end para essa tentativa anterior. */
  erro: string | null;
}

/**
 * R10. Só coleta o motivo: fecha devolvendo o texto aparado, ou `undefined` se
 * cancelado. O envio fica com a tela, que marca apenas a linha em andamento em
 * vez de prender o gerente num diálogo modal enquanto a requisição corre.
 */
@Component({
  selector: 'app-recusar-solicitacao',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MensagemErroComponent,
    DocumentoPipe,
  ],
  template: `
    <h2 mat-dialog-title>Recusar solicitação</h2>

    <form [formGroup]="formulario" (ngSubmit)="confirmar()" novalidate>
      <mat-dialog-content>
        <dl class="dados">
          <div class="dados__item">
            <dt>Cliente</dt>
            <dd>{{ dados.nome }}</dd>
          </div>
          <div class="dados__item">
            <dt>CPF</dt>
            <dd class="numerico">{{ dados.cpf | documento: 'cpf' }}</dd>
          </div>
        </dl>

        <app-mensagem-erro [mensagem]="dados.erro" />

        <mat-form-field appearance="outline" class="campo">
          <mat-label>Motivo da recusa</mat-label>
          <textarea
            matInput
            formControlName="motivo"
            rows="4"
            [attr.maxlength]="maximo"
            cdkFocusInitial
          ></textarea>
          <mat-hint align="end"
            >{{ formulario.controls.motivo.value.length }}/{{ maximo }}</mat-hint
          >
          @if (formulario.controls.motivo.hasError('required')) {
            <mat-error>Informe o motivo da recusa.</mat-error>
          } @else if (formulario.controls.motivo.hasError('motivoCurto')) {
            <mat-error>Descreva o motivo com pelo menos {{ minimo }} caracteres.</mat-error>
          } @else if (formulario.controls.motivo.hasError('maxlength')) {
            <mat-error>O motivo pode ter no máximo {{ maximo }} caracteres.</mat-error>
          }
        </mat-form-field>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button matButton="text" type="button" mat-dialog-close>Cancelar</button>
        <button matButton="filled" type="submit" class="recusar">Recusar solicitação</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .dados {
      margin: 0 0 16px;
      border: 1px solid var(--bantads-borda);
      border-radius: var(--bantads-raio);
      background: var(--bantads-fundo);
    }

    .dados__item {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 14px;
      border-top: 1px solid var(--bantads-borda);
    }

    .dados__item:first-child {
      border-top: 0;
    }

    .dados dt {
      color: var(--bantads-texto-fraco);
      font-size: 13px;
    }

    .dados dd {
      margin: 0;
      font-weight: 500;
      overflow-wrap: anywhere;
    }

    .campo {
      width: 100%;
    }

    .recusar {
      --mat-button-filled-container-color: var(--bantads-erro);
      --mat-button-filled-label-text-color: #ffffff;
    }
  `,
})
export class RecusarSolicitacaoDialogComponent {
  private readonly referencia =
    inject<MatDialogRef<RecusarSolicitacaoDialogComponent, string>>(MatDialogRef);

  protected readonly dados = inject<DadosDaRecusa>(MAT_DIALOG_DATA);
  protected readonly minimo = MOTIVO_MINIMO;
  protected readonly maximo = MOTIVO_MAXIMO;

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    motivo: [
      this.dados.motivo,
      [Validators.required, Validators.maxLength(MOTIVO_MAXIMO), motivoSuficiente],
    ],
  });

  protected confirmar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.referencia.close(this.formulario.controls.motivo.value.trim());
  }
}

/** Conta só o texto aparado: espaços não fazem um motivo. */
function motivoSuficiente(controle: AbstractControl): ValidationErrors | null {
  const valor = typeof controle.value === 'string' ? controle.value.trim() : '';

  if (valor.length === 0) {
    return controle.value === '' ? null : { required: true };
  }

  return valor.length < MOTIVO_MINIMO ? { motivoCurto: true } : null;
}
