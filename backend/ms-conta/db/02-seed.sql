-- Seed do MS Conta — dados pré-cadastrados (docs/specs/05-dados-pre-cadastrados.md).
--
-- Popula os DOIS lados do CQRS de forma consistente: o event store (replay
-- reproduz exatamente os saldos abaixo) e o read model (projeção equivalente).
-- Datas/horas de movimentação convertidas para America/Sao_Paulo (UTC-3).
--
-- Nota: a linha de ms_conta.contas é inserida logo após o evento Criado de
-- cada conta (com o saldo final já conhecido, por ser um seed estático) para
-- satisfazer a FK de ms_conta.movimentacoes(numero_conta) antes das
-- movimentações seguintes serem inseridas.

DO $$
DECLARE
    v_evt BIGINT;
BEGIN

    -- =========================================================================
    -- Catharyna — conta 1291 — gerente Geniéve (98574307084)
    -- =========================================================================
    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('1291', 'Criado',
            '{"cpfCliente":"12912861012","cpfGerente":"98574307084"}'::jsonb,
            1, '2000-01-01 00:00:00-03');

    INSERT INTO ms_conta.contas (numero_conta, cpf_cliente, data_criacao, saldo, cpf_gerente, ultima_versao_aplicada)
    VALUES ('1291', '12912861012', '2000-01-01', 800.0000, '98574307084', 8);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('1291', 'Depósito', '{"valor":"1000.0000"}'::jsonb, 2, '2020-01-01 10:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('1291', '2020-01-01 10:00:00-03', 'depósito', 1000.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('1291', 'Depósito', '{"valor":"900.0000"}'::jsonb, 3, '2020-01-01 11:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('1291', '2020-01-01 11:00:00-03', 'depósito', 900.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('1291', 'Saque', '{"valor":"550.0000"}'::jsonb, 4, '2020-01-01 12:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('1291', '2020-01-01 12:00:00-03', 'saque', 550.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('1291', 'Saque', '{"valor":"350.0000"}'::jsonb, 5, '2020-01-01 13:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('1291', '2020-01-01 13:00:00-03', 'saque', 350.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('1291', 'Depósito', '{"valor":"2000.0000"}'::jsonb, 6, '2020-01-10 15:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('1291', '2020-01-10 15:00:00-03', 'depósito', 2000.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('1291', 'Saque', '{"valor":"500.0000"}'::jsonb, 7, '2020-01-15 08:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('1291', '2020-01-15 08:00:00-03', 'saque', 500.0000, v_evt);

    -- Transferência Catharyna (1291) -> Cleuddônio (0950), R$ 1.700,00.
    -- Dois eventos atômicos (R6 não é SAGA), um em cada stream de conta.
    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('1291', 'TransferênciaOrigem',
            '{"valor":"1700.0000","contaDestino":"0950","cpfDestino":"09506382000","nomeDestino":"Cleuddônio"}'::jsonb,
            8, '2020-01-20 12:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes
        (numero_conta, data_hora, tipo, cpf_origem, nome_origem, cpf_destino, nome_destino, valor, evento_id)
    VALUES ('1291', '2020-01-20 12:00:00-03', 'transferência',
            '12912861012', 'Catharyna', '09506382000', 'Cleuddônio', 1700.0000, v_evt);

    -- =========================================================================
    -- Cleuddônio — conta 0950 — gerente Godophredo (64065268052)
    -- =========================================================================
    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('0950', 'Criado',
            '{"cpfCliente":"09506382000","cpfGerente":"64065268052"}'::jsonb,
            1, '1990-10-10 00:00:00-03');

    INSERT INTO ms_conta.contas (numero_conta, cpf_cliente, data_criacao, saldo, cpf_gerente, ultima_versao_aplicada)
    VALUES ('0950', '09506382000', '1990-10-10', 10000.0000, '64065268052', 7);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('0950', 'TransferênciaDestino',
            '{"valor":"1700.0000","contaOrigem":"1291","cpfOrigem":"12912861012","nomeOrigem":"Catharyna"}'::jsonb,
            2, '2020-01-20 12:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes
        (numero_conta, data_hora, tipo, cpf_origem, nome_origem, cpf_destino, nome_destino, valor, evento_id)
    VALUES ('0950', '2020-01-20 12:00:00-03', 'transferência',
            '12912861012', 'Catharyna', '09506382000', 'Cleuddônio', 1700.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('0950', 'Depósito', '{"valor":"1000.0000"}'::jsonb, 3, '2025-01-01 12:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('0950', '2025-01-01 12:00:00-03', 'depósito', 1000.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('0950', 'Depósito', '{"valor":"5000.0000"}'::jsonb, 4, '2025-01-02 10:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('0950', '2025-01-02 10:00:00-03', 'depósito', 5000.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('0950', 'Saque', '{"valor":"200.0000"}'::jsonb, 5, '2025-01-10 10:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('0950', '2025-01-10 10:00:00-03', 'saque', 200.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('0950', 'Depósito', '{"valor":"7000.0000"}'::jsonb, 6, '2025-02-05 10:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('0950', '2025-02-05 10:00:00-03', 'depósito', 7000.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('0950', 'Saque', '{"valor":"4500.0000"}'::jsonb, 7, '2025-03-06 11:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('0950', '2025-03-06 11:00:00-03', 'saque', 4500.0000, v_evt);

    -- =========================================================================
    -- Catianna — conta 8573 — gerente Gyândula (723862179060)
    -- =========================================================================
    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('8573', 'Criado',
            '{"cpfCliente":"85733854057","cpfGerente":"723862179060"}'::jsonb,
            1, '2012-12-12 00:00:00-03');

    INSERT INTO ms_conta.contas (numero_conta, cpf_cliente, data_criacao, saldo, cpf_gerente, ultima_versao_aplicada)
    VALUES ('8573', '85733854057', '2012-12-12', 200.0000, '723862179060', 3);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('8573', 'Depósito', '{"valor":"1000.0000"}'::jsonb, 2, '2025-05-05 10:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('8573', '2025-05-05 10:00:00-03', 'depósito', 1000.0000, v_evt);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('8573', 'Saque', '{"valor":"800.0000"}'::jsonb, 3, '2025-05-06 10:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('8573', '2025-05-06 10:00:00-03', 'saque', 800.0000, v_evt);

    -- =========================================================================
    -- Cutardo — conta 5887 — gerente Geniéve (98574307084)
    -- =========================================================================
    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('5887', 'Criado',
            '{"cpfCliente":"58872160006","cpfGerente":"98574307084"}'::jsonb,
            1, '2022-02-22 00:00:00-03');

    INSERT INTO ms_conta.contas (numero_conta, cpf_cliente, data_criacao, saldo, cpf_gerente, ultima_versao_aplicada)
    VALUES ('5887', '58872160006', '2022-02-22', 150000.0000, '98574307084', 2);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('5887', 'Depósito', '{"valor":"150000.0000"}'::jsonb, 2, '2025-06-01 10:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('5887', '2025-06-01 10:00:00-03', 'depósito', 150000.0000, v_evt);

    -- =========================================================================
    -- Coândrya — conta 7617 — gerente Godophredo (64065268052)
    -- =========================================================================
    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('7617', 'Criado',
            '{"cpfCliente":"76179646090","cpfGerente":"64065268052"}'::jsonb,
            1, '2025-01-01 00:00:00-03');

    INSERT INTO ms_conta.contas (numero_conta, cpf_cliente, data_criacao, saldo, cpf_gerente, ultima_versao_aplicada)
    VALUES ('7617', '76179646090', '2025-01-01', 1500.0000, '64065268052', 2);

    INSERT INTO ms_conta.eventos_conta (objeto_id, tipo, payload, versao, "timestamp")
    VALUES ('7617', 'Depósito', '{"valor":"1500.0000"}'::jsonb, 2, '2025-07-01 10:00:00-03')
    RETURNING id INTO v_evt;
    INSERT INTO ms_conta.movimentacoes (numero_conta, data_hora, tipo, valor, evento_id)
    VALUES ('7617', '2025-07-01 10:00:00-03', 'depósito', 1500.0000, v_evt);

END $$;
