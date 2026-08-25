import { HttpRequest } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface RotaPublica {
  metodo: string;
  caminho: string;
}

/** Endpoints acessíveis sem sessão: autenticação e autocadastro. */
const ROTAS_PUBLICAS: readonly RotaPublica[] = [
  { metodo: 'POST', caminho: '/login' },
  { metodo: 'POST', caminho: '/clientes' },
];

export function ehRotaPublica(requisicao: HttpRequest<unknown>): boolean {
  const caminho = caminhoDe(requisicao);

  return ROTAS_PUBLICAS.some(
    (rota) => rota.metodo === requisicao.method.toUpperCase() && rota.caminho === caminho,
  );
}

function caminhoDe(requisicao: HttpRequest<unknown>): string {
  const semParametros = requisicao.url.split('?')[0];

  return semParametros.startsWith(environment.apiUrl)
    ? semParametros.slice(environment.apiUrl.length)
    : semParametros;
}
