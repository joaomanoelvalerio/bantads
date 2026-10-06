const redis = require("./redis");
const { CLIENTE_MS_API_URL, GERENTE_MS_API_URL, CONTA_MS_API_URL, AUTH_MS_API_URL } = require("./URLs");

const PADROES_REDIS = ["job:*", "saga:*", "sessao:*", "revogado:*", "cache:*"];

async function reiniciarServico(nome, url) {
  const resposta = await fetch(`${url}/reboot`, { method: "POST" });
  if (resposta.status !== 200) {
    throw new Error(`${nome} devolveu ${resposta.status} no /reboot`);
  }
  return resposta.json();
}

async function limparRedis() {
  for (const padrao of PADROES_REDIS) {
    let cursor = "0";
    do {
      const [proximo, chaves] = await redis.scan(cursor, "MATCH", padrao, "COUNT", 200);
      if (chaves.length > 0) {
        await redis.del(...chaves);
      }
      cursor = proximo;
    } while (cursor !== "0");
  }
}

async function reiniciar() {
  const resultados = await Promise.allSettled([
    reiniciarServico("MS Cliente", CLIENTE_MS_API_URL),
    reiniciarServico("MS Gerente", GERENTE_MS_API_URL),
    reiniciarServico("MS Conta", CONTA_MS_API_URL),
    reiniciarServico("MS Auth", AUTH_MS_API_URL),
  ]);
  const falha = resultados.find((resultado) => resultado.status === "rejected");
  if (falha) {
    throw falha.reason;
  }
  const [cliente, gerente, conta] = resultados.map((resultado) => resultado.value);
  await limparRedis();
  return { status: "ok", clientes: cliente.clientes, gerentes: gerente.gerentes, contas: conta.contas };
}

module.exports = { reiniciar };
