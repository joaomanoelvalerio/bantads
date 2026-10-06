// Jobs assíncronos no Redis.
// Chave `job:<jobId>`, TTL 5 min. Para SAGAs (R9/R13/R15) o jobId é o
// próprio sagaId — o Orquestrador reescreve esta mesma chave com o desfecho
// (CONCLUIDO/FALHA); aqui só se cria o PENDENTE inicial e se lê o estado.
const redis = require("./redis");

const TTL_SEGUNDOS = 5 * 60;

async function criarJobPendente(jobId, dominio, resourceId) {
  const job = { jobId, status: "PENDENTE", resultType: null, dominio, resourceId, erro: null };
  await redis.set(`job:${jobId}`, JSON.stringify(job), "EX", TTL_SEGUNDOS);
  return job;
}

async function buscarJob(jobId) {
  const bruto = await redis.get(`job:${jobId}`);
  return bruto ? JSON.parse(bruto) : null;
}

/**
 * R16 — o relatório não é SAGA, é uma API Composition simples que o próprio
 * Gateway resolve na hora; mesmo assim o contrato pede o padrão 202+job
 * ("em sistemas reais,
 * com grande volume, esse tipo de relatório costuma ser assíncrono"), então
 * o job já nasce CONCLUIDO em vez de PENDENTE seguido de uma atualização.
 */
async function criarJobConcluidoInline(jobId, resultado) {
  const job = { jobId, status: "CONCLUIDO", resultType: "inline", dominio: null, resourceId: null, erro: null, resultado };
  await redis.set(`job:${jobId}`, JSON.stringify(job), "EX", TTL_SEGUNDOS);
  return job;
}

module.exports = { criarJobPendente, criarJobConcluidoInline, buscarJob };
