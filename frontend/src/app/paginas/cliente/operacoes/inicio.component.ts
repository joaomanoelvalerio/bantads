import { Component } from '@angular/core';

@Component({
  selector: 'app-inicio-cliente',
  template: `<p class="orientacao">Escolha uma operação no menu acima para continuar.</p>`,
  styles: `
    .orientacao {
      margin: 0;
      color: var(--bantads-texto-fraco);
      font-size: 14px;
    }
  `,
})
export class InicioClienteComponent {}
