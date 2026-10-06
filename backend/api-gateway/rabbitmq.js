// Conexão com o RabbitMQ.
// Semana 02: só estabelece a conexão; publicar em `saga.cmd` (Semana 06) vai
// reusar o canal daqui.
const amqp = require("amqplib");

const url = `amqp://${process.env.RABBITMQ_USER || "guest"}:${process.env.RABBITMQ_PASSWORD || "guest"}@${process.env.RABBITMQ_HOST || "rabbitmq"}:${process.env.RABBITMQ_PORT || 5672}`;

let canalPromise = null;

function conectar() {
  canalPromise = amqp
    .connect(url)
    .then((conexao) => {
      console.log("Conectado ao RabbitMQ");
      conexao.on("close", () => {
        console.error("Conexão com o RabbitMQ caiu — tentando de novo em 5s");
        setTimeout(conectar, 5000);
      });
      conexao.on("error", (erro) => console.error("RabbitMQ:", erro.message));
      return conexao.createChannel();
    })
    .catch((erro) => {
      console.error("Falha ao conectar ao RabbitMQ:", erro.message, "— tentando de novo em 5s");
      setTimeout(conectar, 5000);
    });
  return canalPromise;
}

conectar();

function obterCanal() {
  return canalPromise;
}

module.exports = { obterCanal };
