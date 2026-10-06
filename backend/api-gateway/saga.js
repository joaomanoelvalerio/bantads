// Publicação de comandos de SAGA.
// O Gateway só publica em `saga.cmd` e retorna 202 — quem executa os passos é
// o Orquestrador; a resposta final chega pelo job no Redis (jobs.js), não por
// aqui.
const { obterCanal } = require("./rabbitmq");

async function publicarComandoSaga(fila, sagaId, tipo, payload) {
  const canal = await obterCanal();
  if (!canal) {
    throw new Error("Canal RabbitMQ indisponível");
  }
  // assertQueue é idempotente (no-op se a fila já existe com os mesmos
  // argumentos) — declarar aqui deixa o Gateway robusto mesmo se subir antes
  // do Orquestrador, dono "de direito" da fila.
  await canal.assertQueue(fila, { durable: true });

  const mensagem = { sagaId, tipo, timestamp: new Date().toISOString(), payload };
  canal.sendToQueue(fila, Buffer.from(JSON.stringify(mensagem)), { persistent: true });
}

module.exports = { publicarComandoSaga };
