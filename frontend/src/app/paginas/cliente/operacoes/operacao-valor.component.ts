import { Component, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable } from 'rxjs';
import { ErroApi } from '../../../core/models/erro-api.model';
import { ContaService } from '../../../core/services/conta.service';
import { MensagemAvisoComponent } from '../../../shared/components/mensagem-aviso/mensagem-aviso.component';
import { MensagemErroComponent } from '../../../shared/components/mensagem-erro/mensagem-erro.component';
import { apenasDigitos, mascaraValor } from '../../../shared/formato/mascara-valor';
import { MoedaPipe } from '../../../shared/pipes/moeda.pipe';
import { paraDecimal } from '../../../shared/validators/salario.validator';
import { valorValidator } from '../../../shared/validators/valor.validator';
import { ClienteComponent } from '../cliente.component';

export type TipoOperacaoDeValor = 'deposito' | 'saque';

interface TextosDaOperacao {
  titulo: string;
  apoio: string;
  rotuloDoCampo: string;
  acao: string;
  sucesso: string;
}

const TEXTOS: Record<TipoOperacaoDeValor, TextosDaOperacao> = {
  deposito: {
    titulo: 'Depósito',
    apoio: 'O valor é creditado na sua própria conta.',
    rotuloDoCampo: 'Valor do depósito (R$)',
    acao: 'Depositar',
    sucesso: 'Depósito registrado.',
  },
  saque: {
    titulo: 'Saque',
    apoio: 'O valor é debitado da sua própria conta.',
    rotuloDoCampo: 'Valor do saque (R$)',
    acao: 'Sacar',
    sucesso: 'Saque registrado.',
  },
};

/**
 * Tela comum a R4 (depósito) e R5 (saque): as duas operam sempre sobre a conta da
 * sessão, com um único campo de valor. A conta nunca é digitada, então não há como
 * o usuário operar sobre conta de terceiro pela interface.
 */
@Component({
  selector: 'app-operacao-valor',
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
  templateUrl: './operacao-valor.component.html',
  styleUrl: './operacao-valor.component.scss',
})
export class OperacaoValorComponent {
  readonly operacao = input.required<TipoOperacaoDeValor>();

  private readonly contas = inject(ContaService);
  private readonly area = inject(ClienteComponent);

  protected readonly textos = computed<TextosDaOperacao>(() => TEXTOS[this.operacao()]);

  protected readonly conta = this.area.conta;
  protected readonly carregandoConta = this.area.consultando;
  protected readonly erroDaConta = this.area.erro;

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    valor: ['', [Validators.required, valorValidator]],
  });

  /** Valor já normalizado para envio, com duas casas decimais. Nunca é number. */
  protected readonly valorRevisado = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected recarregarConta(): void {
    this.area.consultar();
  }

  protected mascarar(): void {
    const controle = this.formulario.controls.valor;
    controle.setValue(mascaraValor(apenasDigitos(controle.value)));
  }

  protected erroDoServidor(): string | null {
    const mensagem: unknown = this.formulario.controls.valor.getError('servidor');
    return typeof mensagem === 'string' ? mensagem : null;
  }

  /** Primeiro passo do envio: pede a confirmação explícita da operação e do valor. */
  protected revisar(): void {
    if (this.enviando() || this.valorRevisado() !== null) {
      return;
    }

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const valor = paraDecimal(this.formulario.controls.valor.value);

    if (valor === null) {
      return;
    }

    this.erro.set(null);
    this.sucesso.set(null);
    this.valorRevisado.set(valor.toFixed(2));
  }

  protected corrigir(): void {
    if (this.enviando()) {
      return;
    }

    this.valorRevisado.set(null);
  }

  protected confirmar(): void {
    const valor = this.valorRevisado();
    const numero = this.conta()?.numero;

    if (valor === null || numero === undefined || this.enviando()) {
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.executar(numero, valor).subscribe({
      next: () => this.concluir(),
      error: (falha: ErroApi) => {
        this.enviando.set(false);
        this.valorRevisado.set(null);
        this.tratarFalha(falha);
      },
    });
  }

  private executar(numero: string, valor: string): Observable<void> {
    return this.operacao() === 'deposito'
      ? this.contas.depositar(numero, valor)
      : this.contas.sacar(numero, valor);
  }

  private concluir(): void {
    this.enviando.set(false);
    this.valorRevisado.set(null);
    this.sucesso.set(this.textos().sucesso);
    this.formulario.reset();

    // A operação não devolve o novo saldo: o lado de consulta é atualizado por
    // mensageria. Uma reconsulta, sem laço nem polling.
    this.area.consultar();
  }

  private tratarFalha(falha: ErroApi): void {
    // 401 já foi tratado pelo interceptor, que encerra a sessão e leva ao login.
    if (falha.status === 401) {
      return;
    }

    // 422 é saldo insuficiente: a mensagem fica junto ao campo e o valor digitado
    // permanece no formulário.
    if (falha.status === 422) {
      const controle = this.formulario.controls.valor;
      controle.setErrors({ servidor: falha.message });
      controle.markAsTouched();
      return;
    }

    this.erro.set(falha.message);
  }
}
