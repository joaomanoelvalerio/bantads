import { Component } from '@angular/core';
import { OperacaoValorComponent } from './operacao-valor.component';

@Component({
  selector: 'app-deposito',
  imports: [OperacaoValorComponent],
  template: `<app-operacao-valor operacao="deposito" />`,
})
export class DepositoComponent {}
