import { Component, input } from '@angular/core';

@Component({
  selector: 'app-mensagem-aviso',
  templateUrl: './mensagem-aviso.component.html',
  styleUrl: './mensagem-aviso.component.scss',
})
export class MensagemAvisoComponent {
  readonly mensagem = input<string | null>(null);
}
