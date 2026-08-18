import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CabecalhoComponent } from '../cabecalho/cabecalho.component';
import { RodapeComponent } from '../rodape/rodape.component';

@Component({
  selector: 'app-layout-autenticado',
  imports: [RouterOutlet, CabecalhoComponent, RodapeComponent],
  templateUrl: './layout-autenticado.component.html',
  styleUrl: './layout-autenticado.component.scss',
})
export class LayoutAutenticadoComponent {}
