import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { DocumentoPipe } from '../../../shared/pipes/documento.pipe';

export interface DadosDaRemocao {
  nome: string;
  cpf: string;
}

/**
 * R15. Confirmação da ação destrutiva: fecha com `true` só pelo botão de
 * remover. O foco inicial fica em Cancelar, então Enter ou Esc não removem.
 */
@Component({
  selector: 'app-confirmar-remocao',
  imports: [MatButtonModule, MatDialogModule, DocumentoPipe],
  template: `
    <h2 mat-dialog-title>Remover gerente</h2>

    <mat-dialog-content>
      <dl class="dados">
        <div class="dados__item">
          <dt>Gerente</dt>
          <dd>{{ dados.nome }}</dd>
        </div>
        <div class="dados__item">
          <dt>CPF</dt>
          <dd class="numerico">{{ dados.cpf | documento: 'cpf' }}</dd>
        </div>
      </dl>

      <p class="alerta">
        Os clientes deste gerente serão transferidos para outro gerente e notificados por e-mail.
      </p>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button matButton="outlined" type="button" [mat-dialog-close]="false" cdkFocusInitial>
        Cancelar
      </button>
      <button matButton="filled" type="button" class="remover" [mat-dialog-close]="true">
        Remover gerente
      </button>
    </mat-dialog-actions>
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

    .alerta {
      margin: 0;
      padding: 12px 14px;
      border: 1px solid rgba(179, 38, 30, 0.3);
      border-left: 3px solid var(--bantads-erro);
      border-radius: var(--bantads-raio);
      background: rgba(179, 38, 30, 0.06);
      color: #7f1d17;
      font-size: 14px;
    }

    .remover {
      --mat-button-filled-container-color: var(--bantads-erro);
      --mat-button-filled-label-text-color: #ffffff;
    }
  `,
})
export class ConfirmarRemocaoDialogComponent {
  protected readonly dados = inject<DadosDaRemocao>(MAT_DIALOG_DATA);
}
