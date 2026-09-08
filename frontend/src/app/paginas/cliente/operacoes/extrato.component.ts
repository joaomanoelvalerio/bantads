import { Component, effect, inject, signal, untracked } from '@angular/core';
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
import { ErroApi } from '../../../core/models/erro-api.model';
import { ContaService } from '../../../core/services/conta.service';
import { SessaoService } from '../../../core/services/sessao.service';
import { MensagemErroComponent } from '../../../shared/components/mensagem-erro/mensagem-erro.component';
import { MoedaPipe } from '../../../shared/pipes/moeda.pipe';
import { ClienteComponent } from '../cliente.component';
import {
  DIAS_MAXIMOS_DO_PERIODO,
  DiaDoExtrato,
  diaDeIso,
  hojeNoFusoDoBanco,
  montarLinhaDoTempo,
} from './linha-do-tempo';

const DIAS_INICIAIS_DO_FILTRO = 30;

/**
 * R7. O back-end devolve o saldo consolidado anterior ao período e as movimentações;
 * a linha do tempo diária, com os dias sem movimentação, é montada aqui com Luxon e
 * Decimal, sempre no fuso de São Paulo.
 */
@Component({
  selector: 'app-extrato',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MensagemErroComponent,
    MoedaPipe,
  ],
  templateUrl: './extrato.component.html',
  styleUrl: './extrato.component.scss',
})
export class ExtratoComponent {
  private readonly contas = inject(ContaService);
  private readonly sessao = inject(SessaoService);
  private readonly area = inject(ClienteComponent);

  protected readonly conta = this.area.conta;
  protected readonly carregandoConta = this.area.consultando;
  protected readonly erroDaConta = this.area.erro;

  protected readonly formulario = inject(FormBuilder).nonNullable.group(
    {
      dataInicio: [
        hojeNoFusoDoBanco().minus({ days: DIAS_INICIAIS_DO_FILTRO }).toFormat('yyyy-LL-dd'),
        [Validators.required],
      ],
      dataFim: [hojeNoFusoDoBanco().toFormat('yyyy-LL-dd'), [Validators.required]],
    },
    { validators: periodoValidator },
  );

  protected readonly dias = signal<readonly DiaDoExtrato[]>([]);
  protected readonly periodoConsultado = signal<string | null>(null);
  protected readonly consultando = signal(false);
  protected readonly erro = signal<string | null>(null);

  private consultaInicialFeita = false;

  constructor() {
    // A primeira consulta espera o número da conta, que vem da área do cliente.
    effect(() => {
      if (this.conta() === null || this.consultaInicialFeita) {
        return;
      }

      this.consultaInicialFeita = true;
      untracked(() => this.consultar());
    });
  }

  protected recarregarConta(): void {
    this.area.consultar();
  }

  protected get erroDoPeriodo(): string | null {
    const erros = this.formulario.errors;

    if (erros === null || !this.formulario.touched) {
      return null;
    }

    if (erros['periodoInvalido'] === true) {
      return 'Informe datas de início e fim válidas.';
    }

    if (erros['fimAntesDoInicio'] === true) {
      return 'A data de fim não pode ser anterior à data de início.';
    }

    if (erros['periodoLongo'] === true) {
      return `O intervalo entre início e fim não pode passar de ${DIAS_MAXIMOS_DO_PERIODO} dias.`;
    }

    return null;
  }

  protected consultar(): void {
    const numero = this.conta()?.numero;
    const cpf = this.sessao.usuario()?.cpf;

    if (numero === undefined || cpf === undefined || this.consultando()) {
      return;
    }

    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const { dataInicio, dataFim } = this.formulario.getRawValue();
    const inicio = diaDeIso(dataInicio);
    const fim = diaDeIso(dataFim);

    this.consultando.set(true);
    this.erro.set(null);

    this.contas.consultarExtrato(numero, dataInicio, dataFim).subscribe({
      next: (extrato) => {
        this.dias.set(montarLinhaDoTempo(extrato, cpf, inicio, fim));
        this.periodoConsultado.set(
          `${inicio.toFormat('dd/LL/yyyy')} a ${fim.toFormat('dd/LL/yyyy')}`,
        );
        this.consultando.set(false);
      },
      error: (falha: ErroApi) => {
        this.consultando.set(false);
        this.dias.set([]);
        this.periodoConsultado.set(null);

        // 401 já foi tratado pelo interceptor, que encerra a sessão e leva ao login.
        if (falha.status !== 401) {
          this.erro.set(falha.message);
        }
      },
    });
  }
}

function periodoValidator(grupo: AbstractControl): ValidationErrors | null {
  const inicioBruto: unknown = grupo.get('dataInicio')?.value;
  const fimBruto: unknown = grupo.get('dataFim')?.value;

  if (typeof inicioBruto !== 'string' || typeof fimBruto !== 'string') {
    return null;
  }

  if (inicioBruto.length === 0 || fimBruto.length === 0) {
    return null;
  }

  const inicio = diaDeIso(inicioBruto);
  const fim = diaDeIso(fimBruto);

  if (!inicio.isValid || !fim.isValid) {
    return { periodoInvalido: true };
  }

  if (fim.toMillis() < inicio.toMillis()) {
    return { fimAntesDoInicio: true };
  }

  return fim.diff(inicio, 'days').days > DIAS_MAXIMOS_DO_PERIODO ? { periodoLongo: true } : null;
}
