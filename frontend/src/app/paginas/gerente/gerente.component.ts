import { Component, inject } from '@angular/core';
import { SessaoService } from '../../core/services/sessao.service';
import { DocumentoPipe } from '../../shared/pipes/documento.pipe';

@Component({
  selector: 'app-gerente',
  imports: [DocumentoPipe],
  templateUrl: './gerente.component.html',
  styleUrl: './gerente.component.scss',
})
export class GerenteComponent {
  protected readonly sessao = inject(SessaoService);
}
