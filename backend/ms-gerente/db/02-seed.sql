-- Seed do MS Gerente — dados pré-cadastrados (docs/specs/05-dados-pre-cadastrados.md).
-- Gadamântio propositalmente não aparece em nenhuma conta de ms_conta: é o
-- "gerente com 0 clientes/0 contas" usado para testar R9 e R13.

INSERT INTO ms_gerente.gerentes (cpf, nome, email, telefone, ativo) VALUES
    ('98574307084',  'Geniéve',    'ger1@bantads.com.br', '(41) 99999-1001', true),
    ('64065268052',  'Godophredo', 'ger2@bantads.com.br', '(41) 99999-1002', true),
    ('723862179060', 'Gyândula',   'ger3@bantads.com.br', '(41) 99999-1003', true),
    ('40501740066',  'Gadamântio', 'ger4@bantads.com.br', '(41) 99999-1004', true);
