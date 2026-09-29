// Hostnames = nomes dos serviços no docker-compose.yml (resolvidos pela rede
// interna do Docker); os defaults só fazem sentido rodando via compose.
// Para rodar o Gateway fora do Docker (`node index.js` direto), sobrescreva
// via env vars apontando para `localhost:<porta publicada>`.
module.exports = {
  GERENTE_MS_API_URL: process.env.GERENTE_MS_API_URL || "http://ms-gerente:8080",
  CLIENTE_MS_API_URL: process.env.CLIENTE_MS_API_URL || "http://ms-cliente:9090",
  CONTA_MS_API_URL: process.env.CONTA_MS_API_URL || "http://ms-conta:8081",
  AUTH_MS_API_URL: process.env.AUTH_MS_API_URL || "http://ms-auth:8082",
};
