const express = require("express");
const cors = require("cors");
const httpProxy = require("express-http-proxy");
const app = express();
const port = 3000;
const { GERENTE_MS_API_URL, CLIENTE_MS_API_URL } = require("./URLs");

const userServiceProxy = httpProxy(GERENTE_MS_API_URL);
const productsServiceProxy = httpProxy(CLIENTE_MS_API_URL);

app.get("/health", (req, res) => {
  res.status(200).send("OK");
});

app.use(cors());

// app.get('/reboot', (req, res) => {

app.listen(port, () => console.log(`Porta atual: ${port}!`));
