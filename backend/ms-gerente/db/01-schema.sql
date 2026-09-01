-- MS Gerente — schema-per-service: todo o domínio de Gerente vive no schema
-- `ms_gerente`, isolado dos schemas dos demais microsserviços.
--
-- Dados mínimos exigidos por docs/specs/03-decomposicao-subdominio.md. Sem
-- senha aqui: autenticação é responsabilidade do MS Auth (MongoDB).

CREATE SCHEMA IF NOT EXISTS ms_gerente;

CREATE TABLE ms_gerente.gerentes (
    cpf       VARCHAR(14)  PRIMARY KEY,
    nome      VARCHAR(120) NOT NULL,
    email     VARCHAR(160) NOT NULL UNIQUE,
    telefone  VARCHAR(20)  NOT NULL,
    ativo     BOOLEAN      NOT NULL DEFAULT true    -- R15: remoção = seta Inativo, nunca apaga
);

CREATE INDEX idx_gerentes_ativo ON ms_gerente.gerentes (ativo);
