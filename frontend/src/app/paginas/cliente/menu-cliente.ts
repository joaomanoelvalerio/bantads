export interface ItemDeMenu {
  rota: string;
  rotulo: string;
}

export const MENU_CLIENTE: readonly ItemDeMenu[] = [
  { rota: 'deposito', rotulo: 'Depósito' },
  { rota: 'saque', rotulo: 'Saque' },
  { rota: 'transferencia', rotulo: 'Transferência' },
  { rota: 'extrato', rotulo: 'Extrato' },
];
