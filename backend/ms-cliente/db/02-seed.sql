-- Seed do MS Cliente — dados pré-cadastrados (docs/specs/05-dados-pre-cadastrados.md).
-- Endereço é livre por definição do enunciado ("equipe escolhe"); usamos
-- endereços fictícios em Curitiba/PR, distintos por cliente.

INSERT INTO ms_cliente.clientes
    (cpf, nome, email, telefone, salario, logradouro, numero, complemento, cep, cidade, uf)
VALUES
    ('12912861012', 'Catharyna',  'cli1@bantads.com.br', '(41) 99999-0001', 10000.0000, 'Rua XV de Novembro',    '100',  NULL, '80020-310', 'Curitiba', 'PR'),
    ('09506382000', 'Cleuddônio', 'cli2@bantads.com.br', '(41) 99999-0002', 20000.0000, 'Av. Sete de Setembro',  '2000', NULL, '80230-000', 'Curitiba', 'PR'),
    ('85733854057', 'Catianna',   'cli3@bantads.com.br', '(41) 99999-0003', 3000.0000,  'Rua Marechal Deodoro',  '350',  NULL, '80010-010', 'Curitiba', 'PR'),
    ('58872160006', 'Cutardo',    'cli4@bantads.com.br', '(41) 99999-0004', 500.0000,   'Rua Comendador Araújo', '500',  NULL, '80420-000', 'Curitiba', 'PR'),
    ('76179646090', 'Coândrya',   'cli5@bantads.com.br', '(41) 99999-0005', 1500.0000,  'Av. Cândido de Abreu',  '817',  NULL, '80530-000', 'Curitiba', 'PR');

-- Cada cliente do seed já nasce com uma solicitação Aprovada correspondente,
-- coerente com a conta/gerente já atribuídos em ms_conta (ver R9: conta e
-- usuário só existem após aprovação). decidido_em replica a data de criação
-- da conta em docs/specs/05-dados-pre-cadastrados.md.
INSERT INTO ms_cliente.solicitacoes
    (cpf, nome, email, telefone, salario, logradouro, numero, complemento, cep, cidade, uf, status, motivo, decidido_em)
VALUES
    ('12912861012', 'Catharyna',  'cli1@bantads.com.br', '(41) 99999-0001', 10000.0000, 'Rua XV de Novembro',    '100',  NULL, '80020-310', 'Curitiba', 'PR', 'Aprovado', NULL, '2000-01-01 00:00:00-03'),
    ('09506382000', 'Cleuddônio', 'cli2@bantads.com.br', '(41) 99999-0002', 20000.0000, 'Av. Sete de Setembro',  '2000', NULL, '80230-000', 'Curitiba', 'PR', 'Aprovado', NULL, '1990-10-10 00:00:00-03'),
    ('85733854057', 'Catianna',   'cli3@bantads.com.br', '(41) 99999-0003', 3000.0000,  'Rua Marechal Deodoro',  '350',  NULL, '80010-010', 'Curitiba', 'PR', 'Aprovado', NULL, '2012-12-12 00:00:00-03'),
    ('58872160006', 'Cutardo',    'cli4@bantads.com.br', '(41) 99999-0004', 500.0000,   'Rua Comendador Araújo', '500',  NULL, '80420-000', 'Curitiba', 'PR', 'Aprovado', NULL, '2022-02-22 00:00:00-03'),
    ('76179646090', 'Coândrya',   'cli5@bantads.com.br', '(41) 99999-0005', 1500.0000,  'Av. Cândido de Abreu',  '817',  NULL, '80530-000', 'Curitiba', 'PR', 'Aprovado', NULL, '2025-01-01 00:00:00-03');
