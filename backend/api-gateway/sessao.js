// Sessão de login (docs/specs/05-nao-funcionais/04-autenticacao.md): quem
// assina o JWT é o Gateway (a chave secreta só existe aqui); a sessão em si
// vive no Redis, não no token — permite revogar (logout) e expirar por
// inatividade (sliding window) sem esperar o `exp` absoluto do JWT.
const jwt = require("jsonwebtoken");
const crypto = require("crypto");
const redis = require("./redis");

const JWT_SECRET = process.env.JWT_SECRET || "bantads-dev-secret-nunca-use-em-producao";
const JWT_EXPIRES_IN = "8h"; // absoluto, não renovável
const SESSAO_TTL_SEGUNDOS = 30 * 60; // inatividade, sliding window

if (!process.env.JWT_SECRET) {
  console.warn(
    "JWT_SECRET não definido em .env — usando segredo de desenvolvimento. Nunca use isso fora do ambiente local.",
  );
}

/** Cria o JWT + a sessão no Redis (sessao:<jti> e a chave reversa sessao:cpf:<cpf>). */
async function criarSessao(cpf, tipo) {
  const jti = crypto.randomUUID();
  const token = jwt.sign({ cpf, tipo }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN, jwtid: jti });

  await Promise.all([
    redis.set(`sessao:${jti}`, JSON.stringify({ cpf, tipo }), "EX", SESSAO_TTL_SEGUNDOS),
    redis.set(`sessao:cpf:${cpf}`, jti, "EX", SESSAO_TTL_SEGUNDOS),
  ]);

  return token;
}

/**
 * Verifica assinatura + exp do JWT e a existência da sessão no Redis (não
 * revogada); renova o TTL da sessão a cada requisição (sliding window) —
 * pipeline de docs/specs/05-nao-funcionais/03-api-gateway.md.
 * Retorna { cpf, tipo, jti } se válido, ou null.
 */
async function validarToken(token) {
  let payload;
  try {
    payload = jwt.verify(token, JWT_SECRET);
  } catch {
    return null;
  }

  const [revogado, sessaoBruta] = await Promise.all([
    redis.get(`revogado:${payload.jti}`),
    redis.get(`sessao:${payload.jti}`),
  ]);

  if (revogado || !sessaoBruta) {
    return null;
  }

  await Promise.all([
    redis.expire(`sessao:${payload.jti}`, SESSAO_TTL_SEGUNDOS),
    redis.expire(`sessao:cpf:${payload.cpf}`, SESSAO_TTL_SEGUNDOS),
  ]);

  return { cpf: payload.cpf, tipo: payload.tipo, jti: payload.jti };
}

/**
 * Logout: apaga as duas chaves da sessão e marca o jti como revogado até o
 * JWT expirar por conta própria (evita que um token ainda "válido" (exp não
 * vencido) volte a autenticar se alguém o reapresentar).
 */
async function encerrarSessao(token, cpf, jti) {
  const payload = jwt.decode(token);
  const segundosRestantes = payload?.exp ? Math.max(payload.exp - Math.floor(Date.now() / 1000), 1) : SESSAO_TTL_SEGUNDOS;

  await Promise.all([
    redis.del(`sessao:${jti}`),
    redis.del(`sessao:cpf:${cpf}`),
    redis.set(`revogado:${jti}`, "1", "EX", segundosRestantes),
  ]);
}

module.exports = { criarSessao, validarToken, encerrarSessao };
