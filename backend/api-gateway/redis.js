// Conexão com o Redis (cache, tokens, jobs, estado de SAGA). Semana 02: só estabelece a
// conexão; sessão de login (Semana 03) e jobs assíncronos (Semana 06) vão
// reusar este client.
const Redis = require("ioredis");

const redis = new Redis({
  host: process.env.REDIS_HOST || "redis",
  port: Number(process.env.REDIS_PORT) || 6379,
  lazyConnect: true,
  retryStrategy: (tentativa) => Math.min(tentativa * 500, 5000),
});

redis.on("error", (erro) => console.error("Redis:", erro.message));

redis
  .connect()
  .then(() => console.log("Conectado ao Redis"))
  .catch((erro) => console.error("Falha ao conectar ao Redis:", erro.message));

module.exports = redis;
