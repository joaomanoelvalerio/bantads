import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ErroApi } from '../../../core/models/erro-api.model';
import { ContaService } from '../../../core/services/conta.service';
import { MensagemAvisoComponent } from '../../../shared/components/mensagem-aviso/mensagem-aviso.component';
import { MensagemErroComponent } from '../../../shared/components/mensagem-erro/mensagem-erro.component';
import {
  apenasDigitos,
  mascaraNumeroDeConta,
  mascaraValor,
} from '../../../shared/formato/mascara-valor';
import { MoedaPipe } from '../../../shared/pipes/moeda.pipe';
import { paraDecimal } from '../../../shared/validators/salario.validator';
import { valorValidator } from '../../../shared/validators/valor.validator';
import { ClienteComponent } from '../cliente.component';

interface TransferenciaRevisada {
  contaDestino: string;
  valor: string;
}

/**
 * R6. O número da conta destino é string de quatro dígitos e preserva zero à
 * esquerda. O front não consulta nem envia nome de cliente: o enriquecimento com
 * CPF e nomes é responsabilidade do API Gateway.
 */
@Component({
  selector: 'app-transferencia',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MensagemAvisoComponent,
    MensagemErroComponent,
    MoedaPipe,
  ],
  templateUrl: './transferencia.component.html',
  styleUrl: './transferencia.component.scss',
})
export class TransferenciaComponent {
  private readonly contas = inject(ContaService);
  private readonly area = inject(ClienteComponent);

  protected readonly conta = this.area.conta;
  protected readonly carregandoConta = this.area.consultando;
  protected readonly erroDaConta = this.area.erro;

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    contaDestino: ['', [Validators.required, Validators.pattern(/^\d{4}$/)]],
    valor: ['', [Validators.required, valorValidator]],
  });

  protected readonly revisada = signal<TransferenciaRevisada | null>(null);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected recarregarConta(): void {
    this.area.consultar();
  }

  protected mascararContaDestino(): void {
    const controle = this.formulario.controls.contaDestino;
    controle.setValue(mascaraNumeroDeConta(apenasDigitos(controle.value)));
  }

  protected mascararValor(): void {
    const controle = this.formulario.controls.valor;
    controle.setValue(mascaraValor(apenasDigitos(controle.value)));
  }

  protected erroDoServidor(campo: 'contaDestino' | 'valor'): string | null {
    const mensagem: unknown = this.formulario.controls[campo].getError('servidor');
    return typeof mensagem === 'string' ? mensagem : null;
  }

  protected revisar(): void {
    if (this.enviando() || this.revisada() !== null) {
      return;
    }

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const valores = this.formulario.getRawValue();
    const valor = paraDecimal(valores.valor);

    if (valor === null) {
      return;
    }

    this.erro.set(null);
    this.sucesso.set(null);
    this.revisada.set({ contaDestino: valores.contaDestino, valor: valor.toFixed(2) });
  }

  protected corrigir(): void {
    if (this.enviando()) {
      return;
    }

    this.revisada.set(null);
  }

  protected confirmar(): void {
    const revisada = this.revisada();
    const numero = this.conta()?.numero;

    if (revisada === null || numero === undefined || this.enviando()) {
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.contas.transferir(numero, revisada.contaDestino, revisada.valor).subscribe({
      next: () => this.concluir(revisada),
      error: (falha: ErroApi) => {
        this.enviando.set(false);
        this.revisada.set(null);
        this.tratarFalha(falha);
      },
    });
  }

  private concluir(revisada: TransferenciaRevisada): void {
    this.enviando.set(false);
    this.revisada.set(null);
    this.sucesso.set(`Transferência para a conta ${revisada.contaDestino} registrada.`);
    this.formulario.reset();

    // A operação não devolve o novo saldo: uma reconsulta, sem laço nem polling.
    this.area.consultar();
  }

  private tratarFalha(falha: ErroApi): void {
    // 401 já foi tratado pelo interceptor, que encerra a sessão e leva ao login.
    if (falha.status === 401) {
      return;
    }

    // 404 é conta destino inexistente; 422 é saldo insuficiente. Nos dois casos a
    // mensagem fica junto ao campo e o que foi digitado permanece no formulário.
    if (falha.status === 404) {
      this.marcarErroDoServidor('contaDestino', falha.message);
      return;
    }

    if (falha.status === 422) {
      this.marcarErroDoServidor('valor', falha.message);
      return;
    }

    this.erro.set(falha.message);
  }

  private marcarErroDoServidor(campo: 'contaDestino' | 'valor', mensagem: string): void {
    const controle = this.formulario.controls[campo];
    controle.setErrors({ servidor: mensagem });
    controle.markAsTouched();
  }
}
