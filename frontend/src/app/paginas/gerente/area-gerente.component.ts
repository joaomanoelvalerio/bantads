import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MENU_GERENTE } from './menu-gerente';

/** Moldura da área do gerente: menu das funcionalidades e a tela ativa. */
@Component({
  selector: 'app-area-gerente',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './area-gerente.component.html',
  styleUrl: './area-gerente.component.scss',
})
export class AreaGerenteComponent {
  protected readonly itens = MENU_GERENTE;
}
