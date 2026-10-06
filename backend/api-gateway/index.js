require("dotenv").config();

const express = require("express");
const cors = require("cors");
const httpProxy = require("express-http-proxy");
const { GERENTE_MS_API_URL, CLIENTE_MS_API_URL, CONTA_MS_API_URL, AUTH_MS_API_URL } = require("./URLs");
const sessao = require("./sessao");
const jobs = require("./jobs");
const { publicarComandoSaga } = require("./saga");
const cache = require("./cache");
const links = require("./links");
const reboot = require("./reboot");

// Conexão de infra (Semana 02) — sessão de login (S3) e SAGA (S6) reusam isto.
require("./redis");
require("./rabbitmq");

const app = express();
const port = process.env.PORT || 3000;

// Precisa vir antes de qualquer rota — senão respostas de /health não levam
// os headers de CORS.
app.use(cors());

app.get("/health", (req, res) => {
  res.status(200).send("OK");
});

// POST /reboot — público, sem autenticação.
app.post("/reboot", async (req, res) => {
  try {
    res.status(200).json(await reboot.reiniciar());
  } catch (erro) {
    console.error("Falha no /reboot:", erro.message);
    res.status(502).json({ status: "erro", message: "Falha ao reiniciar os serviços." });
  }
});

// R2 — Login: API Composition.
// `express.json()` só nesta rota, não globalmente — os proxies abaixo
// precisam repassar o corpo bruto das requisições como chegou.
app.post("/login", express.json(), async (req, res) => {
  const { email, senha } = req.body ?? {};
  if (!email || !senha) {
    return res.status(401).json({ auth: false, message: "Login inválido!" });
  }

  let identidade;
  try {
    const respostaAuth = await fetch(`${AUTH_MS_API_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ login: email, senha }),
    });
    if (respostaAuth.status !== 200) {
      return res.status(401).json({ auth: false, message: "Login inválido!" });
    }
    identidade = await respostaAuth.json();
  } catch (erro) {
    console.error("Falha ao consultar MS Auth:", erro.message);
    return res.status(401).json({ auth: false, message: "Login inválido!" });
  }

  // Nome/e-mail não vêm do MS Auth (que não guarda isso) — compostos aqui
  // consultando o MS do perfil correspondente pelo CPF autenticado.
  const { cpf, tipo } = identidade;
  const ehGerente = tipo === "GERENTE";
  const msUrl = ehGerente ? GERENTE_MS_API_URL : CLIENTE_MS_API_URL;
  const caminho = ehGerente ? "gerentes" : "clientes";

  let dados;
  try {
    const respostaDados = await fetch(`${msUrl}/${caminho}/${cpf}`);
    if (respostaDados.status !== 200) {
      throw new Error(`MS ${caminho} devolveu ${respostaDados.status} para CPF já autenticado`);
    }
    dados = await respostaDados.json();
  } catch (erro) {
    console.error("Falha ao compor dados do usuário no login:", erro.message);
    return res.status(401).json({ auth: false, message: "Login inválido!" });
  }

  const token = await sessao.criarSessao(cpf, tipo);

  return res.json({
    auth: true,
    token,
    tipo,
    usuario: { cpf: dados.cpf, nome: dados.nome, email: dados.email },
  });
});

// Roteamento (Semana 02): repassa para os MSs que já existem e respondem.
// `proxyReqPathResolver` reconstrói o prefixo porque o Express já tira
// `/clientes` de req.url antes de entrar no proxy — sem isso a requisição
// chegaria ao MS sem o prefixo que as rotas dele esperam.
// `proxyReqOptDecorator` injeta a identidade (Semana 03) para os MSs
// confiarem sem revalidar o JWT.
function proxyPara(caminho, destino) {
  return httpProxy(destino, {
    // Numa requisição pro path-raiz do mount (ex.: POST /clientes em si, sem
    // sub-recurso), o Express deixa req.url = "/" — concatenar direto geraria
    // "/clientes/" com barra final, que não bate com o mapeamento exato do
    // Spring e virava 404 (achado testando R1 na Semana 04).
    proxyReqPathResolver: (req) => `${caminho}${req.url === "/" ? "" : req.url}`,
    proxyReqOptDecorator: (proxyReqOpts, srcReq) => {
      if (srcReq.usuario) {
        proxyReqOpts.headers["X-User-CPF"] = srcReq.usuario.cpf;
        proxyReqOpts.headers["X-User-Tipo"] = srcReq.usuario.tipo;
      }
      return proxyReqOpts;
    },
    userResDecorator: async (proxyRes, proxyResData, userReq) => {
      if (caminho === "/gerentes" && userReq.method === "PUT" && proxyRes.statusCode < 300) {
        await cache.invalidar(cache.chaveGerente(decodeURIComponent(userReq.url.slice(1))));
      }
      return links.reescreverLinksDoCorpo(proxyResData, proxyRes.headers["content-type"], links.origemPublica(userReq));
    },
  });
}

// Rotas de área do gerente —
// "restrições por perfil entram rota a rota conforme cada requisito exigir".
// Só o path raiz de /clientes e /gerentes entra aqui — GET /clientes/{cpf} e
// GET /gerentes/{cpf} continuam abertos a qualquer sessão (usados na
// composição do próprio login e na resolução do recurso de um job do R9).
const ROTAS_SOMENTE_GERENTE = [
  { metodo: "GET", padrao: /^\/solicitacoes\/?$/ }, // R8
  { metodo: "POST", padrao: /^\/solicitacoes\/[^/]+\/aprovar$/ }, // R9
  { metodo: "POST", padrao: /^\/solicitacoes\/[^/]+\/rejeitar$/ }, // R10
  { metodo: "GET", padrao: /^\/clientes\/?$/ }, // R11
  { metodo: "GET", padrao: /^\/gerentes\/?$/ }, // R12
  { metodo: "POST", padrao: /^\/gerentes\/?$/ }, // R13
  { metodo: "PUT", padrao: /^\/gerentes\/[^/]+$/ }, // R14
  { metodo: "DELETE", padrao: /^\/gerentes\/[^/]+$/ }, // R15
  { metodo: "GET", padrao: /^\/relatorios\/clientes$/ }, // R16
];

function exigeSessaoDeGerente(req) {
  return ROTAS_SOMENTE_GERENTE.some((rota) => rota.metodo === req.method && rota.padrao.test(req.path));
}

// A partir daqui, toda rota exige sessão válida — exceto o autocadastro
// (POST /clientes), a única escrita pública além do login
// (R1; front espelha isto em
// rotas-publicas.ts).
app.use(async (req, res, next) => {
  if (req.method === "POST" && req.path === "/clientes") {
    return next();
  }

  const token = req.headers["x-access-token"];
  if (!token) {
    return res.status(401).json({ auth: false, message: "Token não fornecido." });
  }

  const usuario = await sessao.validarToken(token);
  if (!usuario) {
    return res.status(401).json({ auth: false, message: "Falha ao autenticar o token." });
  }

  if (exigeSessaoDeGerente(req) && usuario.tipo !== "GERENTE") {
    return res.status(403).json({ message: "Acesso restrito a gerentes." });
  }

  req.usuario = usuario;
  next();
});

app.post("/logout", async (req, res) => {
  await sessao.encerrarSessao(req.headers["x-access-token"], req.usuario.cpf, req.usuario.jti);
  res.sendStatus(200);
});

async function nomeDoCliente(cpf) {
  try {
    const resposta = await fetch(`${CLIENTE_MS_API_URL}/clientes/${encodeURIComponent(cpf)}`);
    if (resposta.status !== 200) {
      return null;
    }
    const dados = await resposta.json();
    return dados.nome ?? null;
  } catch (erro) {
    console.error(`Falha ao consultar nome do cliente ${cpf}:`, erro.message);
    return null;
  }
}

// R6 — enriquecimento da transferência:
// o front só manda contaDestino/valor (não sabe CPF nem nome de quem recebe);
// o MS Conta resolve cpfDestino sozinho, mas nomeOrigem/nomeDestino dependiam
// do Gateway existir para consultar o MS Cliente antes de rotear — até agora
// esses campos ficavam sempre null no extrato.
// Rota específica registrada antes do proxy genérico de /contas, então esta
// intercepta só a transferência; tudo mais sob /contas segue passando direto.
app.post("/contas/:numero/transferencia", express.json(), async (req, res) => {
  const { numero } = req.params;
  const { contaDestino, valor } = req.body ?? {};
  const cpfOrigem = req.usuario.cpf;

  if (!contaDestino || !valor) {
    return res.status(400).json({ message: "contaDestino e valor são obrigatórios." });
  }

  let cpfDestino;
  try {
    const respostaContaDestino = await fetch(`${CONTA_MS_API_URL}/contas/${encodeURIComponent(contaDestino)}`);
    if (respostaContaDestino.status === 404) {
      return res.status(404).json({ message: "Conta destino não encontrada." });
    }
    if (respostaContaDestino.status !== 200) {
      throw new Error(`MS Conta devolveu ${respostaContaDestino.status} ao resolver a conta destino`);
    }
    ({ cpfCliente: cpfDestino } = await respostaContaDestino.json());
  } catch (erro) {
    console.error("Falha ao resolver a conta destino da transferência:", erro.message);
    return res.status(502).json({ message: "Falha ao consultar a conta destino." });
  }

  const [nomeOrigem, nomeDestino] = await Promise.all([
    nomeDoCliente(cpfOrigem),
    nomeDoCliente(cpfDestino),
  ]);

  try {
    const respostaConta = await fetch(`${CONTA_MS_API_URL}/contas/${encodeURIComponent(numero)}/transferencia`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-User-CPF": cpfOrigem,
        "X-User-Tipo": req.usuario.tipo,
      },
      body: JSON.stringify({ contaDestino, valor, nomeOrigem, nomeDestino }),
    });
    const corpo = await respostaConta.text();
    res.status(respostaConta.status);
    res.set("Content-Type", respostaConta.headers.get("Content-Type") ?? "application/json");
    res.send(corpo);
  } catch (erro) {
    console.error("Falha ao rotear a transferência enriquecida:", erro.message);
    res.status(502).json({ message: "Falha ao processar a transferência." });
  }
});

// R9 — Aprovar Cliente [SAGA]:
// o Gateway só cria o job (PENDENTE, jobId == sagaId) e publica em `saga.cmd`
// — quem executa os 7 passos é o Orquestrador; devolve 202 imediatamente.
// Rota específica antes do proxy genérico de /solicitacoes, mesmo padrão da
// transferência (R6) acima.
const crypto = require("crypto");

app.post("/solicitacoes/:cpf/aprovar", async (req, res) => {
  const { cpf } = req.params;
  const jobId = crypto.randomUUID();

  try {
    await cache.invalidar(cache.chaveCliente(cpf));
    await jobs.criarJobPendente(jobId, "clientes", cpf);
    await publicarComandoSaga("saga.cmd", jobId, "aprovar-cliente", { cpf });
    res.status(202).json({ jobId });
  } catch (erro) {
    console.error("Falha ao iniciar a SAGA de aprovação de cliente:", erro.message);
    res.status(502).json({ message: "Falha ao iniciar a aprovação." });
  }
});

// GET /jobs/:jobId/status e /result — genéricos, servem qualquer job (SAGAs
// R9/R13/R15, e R16 mais adiante), não só o de aprovar cliente.
app.get("/jobs/:jobId/status", async (req, res) => {
  const job = await jobs.buscarJob(req.params.jobId);
  if (!job) {
    return res.status(404).json({ message: "Job não encontrado (pode ter expirado)." });
  }
  res.json(job);
});

app.get("/jobs/:jobId/result", async (req, res) => {
  const job = await jobs.buscarJob(req.params.jobId);
  if (!job || job.status !== "CONCLUIDO" || job.resultType !== "inline") {
    return res.status(404).json({ message: "Resultado inline não disponível para este job." });
  }
  res.json(job.resultado ?? null);
});

// R13 — Inserir Gerente [SAGA]: mesmo padrão de POST /solicitacoes/:cpf/aprovar
// (R9) acima — cria o job, publica em saga.cmd, devolve 202.
app.post("/gerentes", express.json(), async (req, res) => {
  const { cpf, nome, email, telefone, senha } = req.body ?? {};
  if (!cpf || !nome || !email || !telefone || !senha) {
    return res.status(400).json({ message: "cpf, nome, email, telefone e senha são obrigatórios." });
  }

  const jobId = crypto.randomUUID();
  try {
    await cache.invalidar(cache.chaveGerente(cpf));
    await jobs.criarJobPendente(jobId, "gerentes", cpf);
    await publicarComandoSaga("saga.cmd", jobId, "inserir-gerente", { cpf, nome, email, telefone, senha });
    res.status(202).json({ jobId });
  } catch (erro) {
    console.error("Falha ao iniciar a SAGA de inserção de gerente:", erro.message);
    res.status(502).json({ message: "Falha ao iniciar a inserção do gerente." });
  }
});

// R15 — Remover Gerente [SAGA]: mesmo padrão 202+job de R9/R13, mas com uma
// pré-condição síncrona antes de publicar em saga.cmd — um gerente não pode
// remover a si mesmo (SAGA 3). A regra do último gerente ativo NÃO é checada aqui: depende do
// estado real no MS Gerente no momento da SAGA, então vem como FALHA do job.
app.delete("/gerentes/:cpf", async (req, res) => {
  const { cpf } = req.params;

  if (req.usuario.cpf === cpf) {
    return res.status(403).json({ message: "Não é possível remover o próprio cadastro de gerente." });
  }

  const jobId = crypto.randomUUID();
  try {
    await cache.invalidar(cache.chaveGerente(cpf));
    await jobs.criarJobPendente(jobId, "gerentes", cpf);
    await publicarComandoSaga("saga.cmd", jobId, "remover-gerente", { cpf });
    res.status(202).json({ jobId });
  } catch (erro) {
    console.error("Falha ao iniciar a SAGA de remoção de gerente:", erro.message);
    res.status(502).json({ message: "Falha ao iniciar a remoção." });
  }
});

// R12 — API Composition: MS Gerente (ativos) + MS Conta (quantidade de
// clientes, via GET /contas — endpoint de uso interno, nunca exposto direto
// a um cliente comum, ver o bloqueio explícito logo abaixo).
app.get("/gerentes", async (req, res) => {
  try {
    const [gerentesResp, contasResp] = await Promise.all([
      fetch(`${GERENTE_MS_API_URL}/gerentes`),
      fetch(`${CONTA_MS_API_URL}/contas`),
    ]);
    if (gerentesResp.status !== 200) {
      throw new Error(`MS Gerente devolveu ${gerentesResp.status}`);
    }
    const gerentes = await gerentesResp.json();
    const contas = contasResp.status === 200 ? await contasResp.json() : [];

    const contagemPorCpf = new Map();
    for (const conta of contas) {
      contagemPorCpf.set(conta.cpfGerente, (contagemPorCpf.get(conta.cpfGerente) ?? 0) + 1);
    }

    const listado = gerentes
      .map((gerente) => ({ ...gerente, quantidadeClientes: contagemPorCpf.get(gerente.cpf) ?? 0 }))
      .sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"));

    res.json(links.reescreverLinks(listado, links.origemPublica(req)));
  } catch (erro) {
    console.error("Falha ao compor a listagem de gerentes (R12):", erro.message);
    res.status(502).json({ message: "Falha ao consultar gerentes." });
  }
});

// R11 — API Composition: MS Cliente (todos) + MS Conta (saldo por cliente).
// Busca por CPF/nome é feita pelo front, não há parâmetro aqui.
app.get("/clientes", async (req, res) => {
  try {
    const [clientesResp, contasResp] = await Promise.all([
      fetch(`${CLIENTE_MS_API_URL}/clientes`),
      fetch(`${CONTA_MS_API_URL}/contas`),
    ]);
    if (clientesResp.status !== 200) {
      throw new Error(`MS Cliente devolveu ${clientesResp.status}`);
    }
    const clientes = await clientesResp.json();
    const contas = contasResp.status === 200 ? await contasResp.json() : [];
    const saldoPorCpf = new Map(contas.map((conta) => [conta.cpfCliente, conta.saldo]));
    const origem = links.origemPublica(req);

    const listado = clientes
      .map((cliente) => ({
        cpf: cliente.cpf,
        nome: cliente.nome,
        cidade: cliente.cidade,
        estado: cliente.uf,
        saldo: saldoPorCpf.get(cliente.cpf) ?? null,
        _links: {
          self: links.link(origem, `/clientes/${encodeURIComponent(cliente.cpf)}`),
          conta: links.link(origem, `/contas/cliente/${encodeURIComponent(cliente.cpf)}`),
        },
      }))
      .sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"));

    res.json(listado);
  } catch (erro) {
    console.error("Falha ao compor a listagem de clientes (R11):", erro.message);
    res.status(502).json({ message: "Falha ao consultar clientes." });
  }
});

// R16 — Relatório de Clientes: API Composition (MS Cliente + MS Conta + MS
// Gerente), mas o contrato pede o mesmo formato 202+job das SAGAs (R9/R13) —
// como a composição é rápida e o próprio Gateway resolve, o job já nasce
// CONCLUIDO em vez de PENDENTE seguido de atualização.
app.get("/relatorios/clientes", async (req, res) => {
  const jobId = crypto.randomUUID();
  try {
    const [clientesResp, contasResp, gerentesResp] = await Promise.all([
      fetch(`${CLIENTE_MS_API_URL}/clientes`),
      fetch(`${CONTA_MS_API_URL}/contas`),
      fetch(`${GERENTE_MS_API_URL}/gerentes`),
    ]);
    if (clientesResp.status !== 200) {
      throw new Error(`MS Cliente devolveu ${clientesResp.status}`);
    }
    const clientes = await clientesResp.json();
    const contas = contasResp.status === 200 ? await contasResp.json() : [];
    const gerentes = gerentesResp.status === 200 ? await gerentesResp.json() : [];

    const contaPorCliente = new Map(contas.map((conta) => [conta.cpfCliente, conta]));
    const nomePorGerente = new Map(gerentes.map((gerente) => [gerente.cpf, gerente.nome]));

    const linhas = clientes
      .map((cliente) => {
        const conta = contaPorCliente.get(cliente.cpf);
        return {
          cpf: cliente.cpf,
          nome: cliente.nome,
          email: cliente.email,
          salario: cliente.salario,
          numeroConta: conta?.numero ?? null,
          saldo: conta?.saldo ?? null,
          cpfGerente: conta?.cpfGerente ?? null,
          nomeGerente: conta ? (nomePorGerente.get(conta.cpfGerente) ?? null) : null,
        };
      })
      .sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"));

    await jobs.criarJobConcluidoInline(jobId, linhas);
    res.status(202).json({ jobId });
  } catch (erro) {
    console.error("Falha ao gerar o relatório de clientes (R16):", erro.message);
    res.status(502).json({ message: "Falha ao gerar o relatório." });
  }
});

// GET /contas (raiz) é uso interno só das composições acima — devolve o
// saldo de TODO MUNDO, não pode ficar acessível a um cliente comum através
// do proxy genérico de /contas logo abaixo. Bloqueado explicitamente aqui,
// antes desse proxy; as composições chamam o MS Conta direto (servidor a
// servidor), nunca passam por esta rota.
app.get("/contas", (req, res) => {
  res.status(403).json({ message: "Acesso negado." });
});

async function responderComCache(req, res, chave, url) {
  try {
    const { status, contentType, corpo } = await cache.lerComCache(chave, url);
    res.status(status);
    res.set("Content-Type", contentType ?? "application/json");
    res.send(links.reescreverLinksDoCorpo(corpo, contentType, links.origemPublica(req)));
  } catch (erro) {
    console.error(`Falha ao consultar ${url}:`, erro.message);
    res.status(502).json({ message: "Falha ao consultar o cadastro." });
  }
}

app.get("/clientes/:cpf", (req, res) =>
  responderComCache(req, res, cache.chaveCliente(req.params.cpf), `${CLIENTE_MS_API_URL}/clientes/${encodeURIComponent(req.params.cpf)}`),
);

app.get("/gerentes/:cpf", (req, res) =>
  responderComCache(req, res, cache.chaveGerente(req.params.cpf), `${GERENTE_MS_API_URL}/gerentes/${encodeURIComponent(req.params.cpf)}`),
);

app.use("/clientes", proxyPara("/clientes", CLIENTE_MS_API_URL));
app.use("/gerentes", proxyPara("/gerentes", GERENTE_MS_API_URL));
app.use("/contas", proxyPara("/contas", CONTA_MS_API_URL));
app.use("/solicitacoes", proxyPara("/solicitacoes", CLIENTE_MS_API_URL));

// eslint-disable-next-line no-unused-vars
app.use((erro, req, res, next) => {
  console.error("Erro não tratado:", erro);
  res.status(500).json({ message: "Erro interno no Gateway" });
});

app.listen(port, () => console.log(`Gateway ouvindo na porta ${port}`));
