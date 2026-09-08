import { Component } from '@angular/core';
import { OperacaoValorComponent } from './operacao-valor.component';

@Component({
  selector: 'app-saque',
  imports: [OperacaoValorComponent],
  template: `<app-operacao-valor operacao="saque" />`,
})
export class SaqueComponent {}
