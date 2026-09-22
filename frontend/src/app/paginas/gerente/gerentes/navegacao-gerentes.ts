import { Router } from '@angular/router';

/** Destino absoluto: as telas de cadastro têm rotas de dois segmentos. */
export const ROTA_LISTAGEM_DE_GERENTES = '/gerente/gerentes';

/**
 * Mensagem que uma tela de cadastro entrega à listagem ao voltar para ela. Vai no
 * estado da navegação, não na URL: some ao recarregar a página, como deve.
 */
const CHAVE = 'confirmacao';

export function estadoComConfirmacao(mensagem: string): Record<string, string> {
  return { [CHAVE]: mensagem };
}

export function confirmacaoRecebida(router: Router): string | null {
  const valor: unknown = router.currentNavigation()?.extras.state?.[CHAVE];

  return typeof valor === 'string' ? valor : null;
}
