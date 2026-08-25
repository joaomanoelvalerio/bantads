# Build with:  docker build -t my-app .
# Run with:    docker run -p 3000:3000 my-app

FROM node:lts-alpine

WORKDIR /backend/api-gateway

# Install dependencies first so this layer is cached
COPY package*.json ./
RUN npm install

# Copy the rest of the source
COPY . .

EXPOSE 3000

CMD ["node", "index.js"]
