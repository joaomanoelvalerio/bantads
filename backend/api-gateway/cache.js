const redis = require("./redis");

const TTL_SEGUNDOS = 5 * 60;

const chaveCliente = (cpf) => `cache:cliente:${cpf}`;
const chaveGerente = (cpf) => `cache:gerente:${cpf}`;

async function lerComCache(chave, url) {
  try {
    const emCache = await redis.get(chave);
    if (emCache !== null) {
      return { status: 200, contentType: "application/json", corpo: emCache };
    }
  } catch (erro) {
    console.error(`Falha ao ler ${chave} do cache:`, erro.message);
  }

  const resposta = await fetch(url);
  const corpo = await resposta.text();
  if (resposta.status === 200) {
    try {
      await redis.set(chave, corpo, "EX", TTL_SEGUNDOS);
    } catch (erro) {
      console.error(`Falha ao gravar ${chave} no cache:`, erro.message);
    }
  }
  return { status: resposta.status, contentType: resposta.headers.get("Content-Type"), corpo };
}

async function invalidar(chave) {
  try {
    await redis.del(chave);
  } catch (erro) {
    console.error(`Falha ao invalidar ${chave} — expira sozinha pelo TTL:`, erro.message);
  }
}

module.exports = { chaveCliente, chaveGerente, lerComCache, invalidar };
