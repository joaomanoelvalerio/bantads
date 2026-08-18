import { Component, input } from '@angular/core';

@Component({
  selector: 'app-mensagem-erro',
  templateUrl: './mensagem-erro.component.html',
  styleUrl: './mensagem-erro.component.scss',
})
export class MensagemErroComponent {
  readonly mensagem = input<string | null>(null);
}
