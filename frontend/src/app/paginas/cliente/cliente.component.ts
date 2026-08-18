import { Component, inject } from '@angular/core';
import { SessaoService } from '../../core/services/sessao.service';
import { DocumentoPipe } from '../../shared/pipes/documento.pipe';

@Component({
  selector: 'app-cliente',
  imports: [DocumentoPipe],
  templateUrl: './cliente.component.html',
  styleUrl: './cliente.component.scss',
})
export class ClienteComponent {
  protected readonly sessao = inject(SessaoService);
}
