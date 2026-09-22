import { ItemDeMenu } from '../cliente/menu-cliente';

export interface ItemDeMenuDoGerente extends ItemDeMenu {
  /** Só a rota exata ativa o item; sem isso a rota vazia ficaria sempre ativa. */
  exata: boolean;
}

export const MENU_GERENTE: readonly ItemDeMenuDoGerente[] = [
  { rota: '.', rotulo: 'Solicitações', exata: true },
  { rota: 'clientes', rotulo: 'Clientes', exata: false },
  { rota: 'relatorio-clientes', rotulo: 'Relatório de clientes', exata: false },
  { rota: 'gerentes', rotulo: 'Gerentes', exata: false },
];
