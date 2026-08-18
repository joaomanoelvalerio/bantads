import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { DateTime } from 'luxon';
import { ErroApi } from '../../core/models/erro-api.model';
import { NovoCliente } from '../../core/models/solicitacao.model';
import { ClienteService } from '../../core/services/cliente.service';
import { CabecalhoComponent } from '../../shared/components/cabecalho/cabecalho.component';
import { MensagemErroComponent } from '../../shared/components/mensagem-erro/mensagem-erro.component';
import { RodapeComponent } from '../../shared/components/rodape/rodape.component';
import { MoedaPipe } from '../../shared/pipes/moeda.pipe';
import { cpfValidator } from '../../shared/validators/cpf.validator';
import { paraDecimal, salarioValidator } from '../../shared/validators/salario.validator';

export const UNIDADES_FEDERATIVAS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
  'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
] as const;

interface ResumoDaSolicitacao {
  nome: string;
  email: string;
  salario: string;
  enviadaEm: string;
}

@Component({
  selector: 'app-autocadastro',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    CabecalhoComponent,
    RodapeComponent,
    MensagemErroComponent,
    MoedaPipe,
  ],
  templateUrl: './autocadastro.component.html',
  styleUrl: './autocadastro.component.scss',
})
export class AutocadastroComponent {
  private readonly clientes = inject(ClienteService);

  protected readonly unidadesFederativas = UNIDADES_FEDERATIVAS;

  protected readonly formulario = inject(FormBuilder).nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    cpf: ['', [Validators.required, cpfValidator]],
    telefone: ['', [Validators.required, Validators.pattern(/^\(\d{2}\) \d{4,5}-\d{4}$/)]],
    salario: ['', [Validators.required, salarioValidator]],
    cep: ['', [Validators.required, Validators.pattern(/^\d{5}-\d{3}$/)]],
    logradouro: ['', [Validators.required, Validators.maxLength(120)]],
    numero: ['', [Validators.required, Validators.maxLength(10)]],
    complemento: ['', [Validators.maxLength(60)]],
    cidade: ['', [Validators.required, Validators.maxLength(80)]],
    uf: ['', [Validators.required]],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly resumo = signal<ResumoDaSolicitacao | null>(null);

  protected mascararCpf(): void {
    this.reescrever('cpf', mascaraCpf);
  }

  protected mascararTelefone(): void {
    this.reescrever('telefone', mascaraTelefone);
  }

  protected mascararCep(): void {
    this.reescrever('cep', mascaraCep);
  }

  protected mascararSalario(): void {
    this.reescrever('salario', mascaraSalario);
  }

  protected erroDoServidor(controle: AbstractControl): string | null {
    const mensagem: unknown = controle.getError('servidor');
    return typeof mensagem === 'string' ? mensagem : null;
  }

  protected enviar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const dados = this.montarPayload();

    if (dados === null) {
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.clientes.solicitarAbertura(dados).subscribe({
      next: () => this.concluir(dados),
      error: (falha: ErroApi) => {
        this.enviando.set(false);
        this.direcionarErro(falha.message);
      },
    });
  }

  private montarPayload(): NovoCliente | null {
    const valores = this.formulario.getRawValue();
    const salario = paraDecimal(valores.salario);

    if (salario === null) {
      return null;
    }

    return {
      nome: valores.nome.trim(),
      email: valores.email.trim(),
      cpf: apenasDigitos(valores.cpf),
      telefone: apenasDigitos(valores.telefone),
      salario: salario.toFixed(2),
      cep: apenasDigitos(valores.cep),
      logradouro: valores.logradouro.trim(),
      numero: valores.numero.trim(),
      complemento: valores.complemento.trim(),
      cidade: valores.cidade.trim(),
      uf: valores.uf,
    };
  }

  private concluir(dados: NovoCliente): void {
    this.enviando.set(false);
    this.formulario.disable();
    this.resumo.set({
      nome: dados.nome,
      email: dados.email,
      salario: dados.salario,
      enviadaEm: DateTime.now().setLocale('pt-BR').toFormat("dd/LL/yyyy 'às' HH:mm"),
    });
  }

  private direcionarErro(mensagem: string): void {
    const mencionaCpf = /cpf/i.test(mensagem);
    const mencionaEmail = /e-?mail/i.test(mensagem);

    if (mencionaCpf) {
      this.marcarErroDoServidor(this.formulario.controls.cpf, mensagem);
    }

    if (mencionaEmail) {
      this.marcarErroDoServidor(this.formulario.controls.email, mensagem);
    }

    if (!mencionaCpf && !mencionaEmail) {
      this.erro.set(mensagem);
    }
  }

  private marcarErroDoServidor(controle: AbstractControl, mensagem: string): void {
    controle.setErrors({ ...controle.errors, servidor: mensagem });
    controle.markAsTouched();
  }

  private reescrever(
    campo: 'cpf' | 'telefone' | 'cep' | 'salario',
    formatador: (digitos: string) => string,
  ): void {
    const controle = this.formulario.controls[campo];
    controle.setValue(formatador(apenasDigitos(controle.value)));
  }
}

function apenasDigitos(valor: string): string {
  return valor.replace(/\D/g, '');
}

function mascaraCpf(digitos: string): string {
  const limitado = digitos.slice(0, 11);
  const partes = [limitado.slice(0, 3), limitado.slice(3, 6), limitado.slice(6, 9)].filter(
    (parte) => parte.length > 0,
  );
  const verificador = limitado.slice(9);

  return partes.join('.') + (verificador.length > 0 ? `-${verificador}` : '');
}

function mascaraTelefone(digitos: string): string {
  const limitado = digitos.slice(0, 11);

  if (limitado.length <= 2) {
    return limitado;
  }

  const corte = limitado.length > 10 ? 7 : 6;
  const prefixo = `(${limitado.slice(0, 2)}) ${limitado.slice(2, corte)}`;
  const sufixo = limitado.slice(corte);

  return sufixo.length > 0 ? `${prefixo}-${sufixo}` : prefixo;
}

function mascaraCep(digitos: string): string {
  const limitado = digitos.slice(0, 8);

  return limitado.length > 5 ? `${limitado.slice(0, 5)}-${limitado.slice(5)}` : limitado;
}

function mascaraSalario(digitos: string): string {
  const limitado = digitos.replace(/^0+/, '').slice(0, 11);

  if (limitado.length === 0) {
    return '';
  }

  const centavos = limitado.padStart(3, '0');
  const inteiro = centavos.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, '.');

  return `${inteiro},${centavos.slice(-2)}`;
}
