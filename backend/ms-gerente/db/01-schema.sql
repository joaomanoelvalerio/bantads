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

-- Idempotência de comandos de SAGA (S8) — docs/specs/05-nao-funcionais/07-rabbitmq-filas.md:
-- "deduplicar pelo par (sagaId, tipo)". Só guarda gerente.inserir — reentrega
-- at-least-once não pode criar um segundo gerente; os demais comandos deste
-- serviço já são idempotentes por construção.
CREATE TABLE ms_gerente.comandos_processados (
    saga_id       VARCHAR(100)  NOT NULL,
    tipo          VARCHAR(100)  NOT NULL,
    resposta      JSONB         NOT NULL,
    processado_em TIMESTAMPTZ   NOT NULL DEFAULT now(),
    PRIMARY KEY (saga_id, tipo)
);
