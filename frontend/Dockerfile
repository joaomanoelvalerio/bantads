# syntax=docker/dockerfile:1
# Build de produção do Angular servido como arquivos estáticos pelo nginx —
# nada de `ng serve` na máquina de defesa.
FROM node:lts-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx ng build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
