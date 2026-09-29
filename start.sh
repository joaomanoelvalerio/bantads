#!/usr/bin/env bash
# Builda a imagem Docker de cada microsserviço e sobe a frota inteira via
# docker-compose (Semana 02 do cronograma — docs/specs/10-cronograma.md).
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

if [ ! -f .env ]; then
  echo "Nenhum .env encontrado — copiando de .env.example."
  echo "Preencha as senhas (Postgres, RabbitMQ, SMTP) antes de rodar de novo."
  cp .env.example .env
fi

echo "==> Buildando as imagens..."
docker compose build

echo "==> Subindo a frota (aguardando os healthchecks)..."
docker compose up -d --wait --wait-timeout 180

echo
echo "==> Frota no ar:"
docker compose ps
