-- MS Cliente — schema-per-service: todo o domínio de Cliente vive no schema
-- `ms_cliente`, isolado dos schemas dos demais microsserviços.
--
-- Dados mínimos exigidos por docs/specs/03-decomposicao-subdominio.md. Note que
-- nenhuma das duas tabelas guarda senha — autenticação é responsabilidade do
-- MS Auth (MongoDB), criado somente após aprovação (R9).

CREATE SCHEMA IF NOT EXISTS ms_cliente;

-- Solicitações de autocadastro (R1) — registradas em TODOS os estados
-- (Pendente/Aprovado/Não aprovado) e nunca apagadas; alimentam a tela do
-- gerente (R8).
CREATE TABLE ms_cliente.solicitacoes (
    id           BIGSERIAL     PRIMARY KEY,
    cpf          VARCHAR(14)   NOT NULL,
    nome         VARCHAR(120)  NOT NULL,
    email        VARCHAR(160)  NOT NULL,
    telefone     VARCHAR(20)   NOT NULL,
    salario      NUMERIC(19,4) NOT NULL,
    logradouro   VARCHAR(160)  NOT NULL,
    numero       VARCHAR(20)   NOT NULL,
    complemento  VARCHAR(80),
    cep          VARCHAR(9)    NOT NULL,
    cidade       VARCHAR(120)  NOT NULL,
    uf           VARCHAR(2)    NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'Pendente'
                 CHECK (status IN ('Pendente', 'Aprovado', 'Não aprovado')),
    motivo       VARCHAR(200),                 -- preenchido quando status = Não aprovado
    decidido_em  TIMESTAMPTZ,                   -- data/hora da aprovação ou rejeição
    criado_em    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Suposição (registrar no PDF de suposições — ver docs/design/suposicoes.md):
-- R1 diz que não pode haver "novo autocadastro se já existir um (mesmo que
-- ainda Pendente)". Interpretamos isso como "CPF com solicitação Pendente ou
-- já Aprovada bloqueia nova tentativa"; uma solicitação Não aprovada libera
-- uma nova. Por isso um índice único PARCIAL, não um UNIQUE(cpf) simples.
CREATE UNIQUE INDEX uq_solicitacoes_cpf_ativa
    ON ms_cliente.solicitacoes (cpf)
    WHERE status IN ('Pendente', 'Aprovado');

CREATE INDEX idx_solicitacoes_email ON ms_cliente.solicitacoes (email);
CREATE INDEX idx_solicitacoes_status ON ms_cliente.solicitacoes (status);

-- Dados de cliente já aprovado (criados pela SAGA Aprovar Cliente — R9).
CREATE TABLE ms_cliente.clientes (
    cpf          VARCHAR(14)   PRIMARY KEY,
    nome         VARCHAR(120)  NOT NULL,
    email        VARCHAR(160)  NOT NULL UNIQUE,
    telefone     VARCHAR(20)   NOT NULL,
    salario      NUMERIC(19,4) NOT NULL,
    logradouro   VARCHAR(160)  NOT NULL,
    numero       VARCHAR(20)   NOT NULL,
    complemento  VARCHAR(80),
    cep          VARCHAR(9)    NOT NULL,
    cidade       VARCHAR(120)  NOT NULL,
    uf           VARCHAR(2)    NOT NULL
);
