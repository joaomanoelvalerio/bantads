-- MS Conta — schema-per-service: todo o domínio de Conta vive no schema `ms_conta`,
-- isolado dos schemas dos demais microsserviços dentro do mesmo container Postgres.
--
-- CQRS + Event Sourcing (obrigatório para este MS — ver
-- docs/specs/05-nao-funcionais/10-cqrs.md):
--   - lado COMMAND (ms_conta.eventos_conta): event store, fonte da verdade.
--   - lado QUERY (ms_conta.contas / ms_conta.movimentacoes): read model
--     desnormalizado, sincronizado de forma assíncrona e idempotente via a fila
--     `ms.conta.events`.

CREATE SCHEMA IF NOT EXISTS ms_conta;

-- ---------------------------------------------------------------------------
-- Lado COMMAND — event store
-- ---------------------------------------------------------------------------

CREATE TABLE ms_conta.eventos_conta (
    id          BIGSERIAL     PRIMARY KEY,
    objeto_id   VARCHAR(4)    NOT NULL,              -- número da conta
    tipo        VARCHAR(30)   NOT NULL
                CHECK (tipo IN ('Criado', 'Saque', 'Depósito',
                                 'TransferênciaOrigem', 'TransferênciaDestino',
                                 'GerenteAlterado')),
    payload     JSONB         NOT NULL,               -- valores monetários como string (JSON)
    versao      INTEGER       NOT NULL,               -- ordem por conta — optimistic locking
    "timestamp" TIMESTAMPTZ   NOT NULL,

    CONSTRAINT uq_eventos_conta_objeto_versao UNIQUE (objeto_id, versao)
);

CREATE INDEX idx_eventos_conta_objeto_id ON ms_conta.eventos_conta (objeto_id);

COMMENT ON TABLE ms_conta.eventos_conta IS
    'Event store. Saldo é sempre obtido por replay/fold dos eventos ordenados por '
    'versao — nunca lido do read model (R5/R6 exigem o estado consistente do '
    'command). O payload do evento Criado carrega o CPF do gerente responsável. '
    'Concorrência (ex.: 2 saques simultâneos) é resolvida pela constraint '
    'unique(objeto_id, versao): a transação perdedora falha, refaz o replay e '
    'revalida.';

-- ---------------------------------------------------------------------------
-- Lado QUERY — read model desnormalizado
-- ---------------------------------------------------------------------------

CREATE TABLE ms_conta.contas (
    numero_conta VARCHAR(4)     PRIMARY KEY,
    cpf_cliente  VARCHAR(14)    NOT NULL,
    data_criacao DATE           NOT NULL,
    saldo        NUMERIC(19,4)  NOT NULL DEFAULT 0,
    cpf_gerente  VARCHAR(14)    NOT NULL,

    -- Última versão de ms_conta.eventos_conta já refletida aqui. Usada pela
    -- projeção (consumidor de ms.conta.events, implementado na S4) para ser
    -- idempotente perante a entrega at-least-once do RabbitMQ: eventos com
    -- versao <= ultima_versao_aplicada são ignorados.
    ultima_versao_aplicada INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_contas_cpf_cliente ON ms_conta.contas (cpf_cliente);
CREATE INDEX idx_contas_cpf_gerente ON ms_conta.contas (cpf_gerente);

CREATE TABLE ms_conta.movimentacoes (
    id            BIGSERIAL      PRIMARY KEY,
    numero_conta  VARCHAR(4)     NOT NULL REFERENCES ms_conta.contas (numero_conta),
    data_hora     TIMESTAMPTZ    NOT NULL,
    tipo          VARCHAR(20)    NOT NULL
                  CHECK (tipo IN ('depósito', 'saque', 'transferência')),
    cpf_origem    VARCHAR(14),
    nome_origem   VARCHAR(120),
    cpf_destino   VARCHAR(14),
    nome_destino  VARCHAR(120),
    valor         NUMERIC(19,4)  NOT NULL,

    -- Rastreia o evento de origem da linha, para a projeção detectar
    -- reentrega (at-least-once) sem duplicar a linha no extrato.
    evento_id     BIGINT         REFERENCES ms_conta.eventos_conta (id)
);

CREATE INDEX idx_movimentacoes_conta_data ON ms_conta.movimentacoes (numero_conta, data_hora);

COMMENT ON TABLE ms_conta.movimentacoes IS
    'Histórico para o extrato (R7). cpf/nome_origem e cpf/nome_destino só são '
    'preenchidos quando tipo = transferência — enriquecidos pelo API Gateway '
    'antes de rotear a requisição (ver docs/specs/05-nao-funcionais/10-cqrs.md), '
    'pois o MS Conta não conhece nomes de clientes. Uma transferência gera uma '
    'linha em cada conta (origem e destino); o front decide a cor '
    '(vermelho/azul) comparando numero_conta ao lado origem/destino da linha.';
