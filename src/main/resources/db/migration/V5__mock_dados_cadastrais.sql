-- ============================================================
-- V5: Dados mockados para testes de navegação — empresa_id = 1
-- Script idempotente: pode ser executado múltiplas vezes.
-- Tabelas com UNIQUE constraint: ON CONFLICT DO NOTHING
-- Tabelas sem UNIQUE: INSERT ... WHERE NOT EXISTS
-- ============================================================


-- ============================================================
-- 1. GRUPOS DE PRODUTOS (8 grupos)
--    UNIQUE (empresa_id, nome) → ON CONFLICT DO NOTHING
-- ============================================================

INSERT INTO grupo_produto (empresa_id, nome, ativo) VALUES
    (1, 'Alimentos',       true),
    (1, 'Bebidas',         true),
    (1, 'Limpeza',         true),
    (1, 'Higiene Pessoal', true),
    (1, 'Eletrônicos',     true),
    (1, 'Vestuário',       true),
    (1, 'Ferramentas',     true),
    (1, 'Papelaria',       true)
ON CONFLICT (empresa_id, nome) DO NOTHING;


-- ============================================================
-- 2. UNIDADES DE MEDIDA
--    Já inseridas em V2/V3 — ON CONFLICT DO NOTHING garante
--    que não falha mesmo se já existirem.
-- ============================================================

INSERT INTO unidade_medida (empresa_id, sigla, descricao) VALUES
    (1, 'UN',  'Unidade'),
    (1, 'KG',  'Quilograma'),
    (1, 'G',   'Grama'),
    (1, 'L',   'Litro'),
    (1, 'ML',  'Mililitro'),
    (1, 'CX',  'Caixa'),
    (1, 'PC',  'Peça'),
    (1, 'MT',  'Metro'),
    (1, 'DZ',  'Dúzia'),
    (1, 'SC',  'Saco')
ON CONFLICT (empresa_id, sigla) DO NOTHING;


-- ============================================================
-- 3. PRODUTOS (20 produtos)
--    UNIQUE (empresa_id, codigo_interno) → ON CONFLICT DO NOTHING
--    margem_lucro = ((venda - custo) / custo) * 100
--    preco_minimo = custo * 1.05
-- ============================================================

-- 3.1 ALIMENTOS (5 produtos)

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Alimentos'  AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'KG'        AND empresa_id = 1),
    'ALC001', '7891234560001',
    'Arroz Tipo 1 Longo Fino 5kg',
    18.5000, 34.5946, 24.9000, 19.4250,
    150.0000, 20.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'ALC001'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Alimentos'  AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'KG'        AND empresa_id = 1),
    'ALC002', '7891234560002',
    'Feijão Carioca Tipo 1 1kg',
    6.8000, 45.5882, 9.9000, 7.1400,
    200.0000, 30.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'ALC002'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Alimentos'  AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'KG'        AND empresa_id = 1),
    'ALC003', '7891234560003',
    'Açúcar Cristal 1kg',
    3.2000, 55.9375, 4.9900, 3.3600,
    180.0000, 25.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'ALC003'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Alimentos'  AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'        AND empresa_id = 1),
    'ALC004', '7891234560004',
    'Macarrão Espaguete 500g',
    2.9000, 54.8276, 4.4900, 3.0450,
    120.0000, 20.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'ALC004'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Alimentos'  AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'        AND empresa_id = 1),
    'ALC005', '7891234560005',
    'Óleo de Soja 900ml',
    5.4000, 47.9630, 7.9900, 5.6700,
    90.0000, 15.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'ALC005'
);

-- 3.2 BEBIDAS (4 produtos)

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Bebidas'    AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'        AND empresa_id = 1),
    'BEB001', '7891234560006',
    'Água Mineral 500ml',
    0.8000, 148.7500, 1.9900, 0.8400,
    500.0000, 50.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'BEB001'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Bebidas'    AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'        AND empresa_id = 1),
    'BEB002', '7891234560007',
    'Refrigerante Cola 2L',
    4.2000, 66.4286, 6.9900, 4.4100,
    80.0000, 20.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'BEB002'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Bebidas'    AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'        AND empresa_id = 1),
    'BEB003', '7891234560008',
    'Suco de Laranja 1L',
    5.5000, 54.3636, 8.4900, 5.7750,
    60.0000, 15.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'BEB003'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Bebidas'    AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'        AND empresa_id = 1),
    'BEB004', '7891234560009',
    'Café Torrado Moído 500g',
    12.0000, 57.5000, 18.9000, 12.6000,
    45.0000, 10.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'BEB004'
);

-- 3.3 LIMPEZA (3 produtos — último com estoque abaixo do mínimo)

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Limpeza'    AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'        AND empresa_id = 1),
    'LMP001', '7891234560010',
    'Detergente Líquido 500ml',
    1.8000, 66.1111, 2.9900, 1.8900,
    200.0000, 30.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'LMP001'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Limpeza'    AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'        AND empresa_id = 1),
    'LMP002', '7891234560011',
    'Água Sanitária 1L',
    2.1000, 66.1905, 3.4900, 2.2050,
    150.0000, 20.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'LMP002'
);

-- Estoque ABAIXO do mínimo (8 < 15) → para testar alerta
INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Limpeza'    AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'        AND empresa_id = 1),
    'LMP003', '7891234560012',
    'Sabão em Pó 1kg',
    7.5000, 59.8667, 11.9900, 7.8750,
    8.0000, 15.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'LMP003'
);

-- 3.4 HIGIENE PESSOAL (3 produtos — último com estoque zerado)

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Higiene Pessoal' AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'             AND empresa_id = 1),
    'HIG001', '7891234560013',
    'Shampoo 400ml',
    8.9000, 68.4270, 14.9900, 9.3450,
    55.0000, 10.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'HIG001'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Higiene Pessoal' AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'             AND empresa_id = 1),
    'HIG002', '7891234560014',
    'Sabonete Barra 90g',
    1.5000, 66.0000, 2.4900, 1.5750,
    300.0000, 40.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'HIG002'
);

-- Estoque ZERADO (0 < 20) → para testar alerta crítico
INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Higiene Pessoal' AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'             AND empresa_id = 1),
    'HIG003', '7891234560015',
    'Pasta de Dente 90g',
    3.8000, 57.6316, 5.9900, 3.9900,
    0.0000, 20.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'HIG003'
);

-- 3.5 ELETRÔNICOS (3 produtos)

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Eletrônicos' AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'         AND empresa_id = 1),
    'ELE001', '7891234560016',
    'Cabo USB-C 1m',
    12.0000, 149.1667, 29.9000, 12.6000,
    35.0000, 5.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'ELE001'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Eletrônicos' AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'         AND empresa_id = 1),
    'ELE002', '7891234560017',
    'Carregador Universal 10W',
    28.0000, 113.9286, 59.9000, 29.4000,
    18.0000, 5.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'ELE002'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Eletrônicos' AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'         AND empresa_id = 1),
    'ELE003', '7891234560018',
    'Fone de Ouvido com Fio',
    15.0000, 166.0000, 39.9000, 15.7500,
    22.0000, 5.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'ELE003'
);

-- 3.6 PAPELARIA (2 produtos)

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Papelaria' AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'CX'       AND empresa_id = 1),
    'PAP001', '7891234560019',
    'Caneta Esferográfica Azul cx12',
    8.4000, 77.3810, 14.9000, 8.8200,
    40.0000, 10.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'PAP001'
);

INSERT INTO produto (
    empresa_id, grupo_id, unidade_id, codigo_interno, codigo_barras,
    descricao, preco_custo, margem_lucro, preco_venda, preco_minimo,
    estoque_atual, estoque_minimo, ativo, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM grupo_produto WHERE nome = 'Papelaria' AND empresa_id = 1),
    (SELECT id FROM unidade_medida WHERE sigla = 'UN'       AND empresa_id = 1),
    'PAP002', '7891234560020',
    'Caderno Universitário 200fls',
    14.0000, 77.8571, 24.9000, 14.7000,
    25.0000, 8.0000, true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM produto WHERE empresa_id = 1 AND codigo_interno = 'PAP002'
);


-- ============================================================
-- 4. FORNECEDORES (8 fornecedores)
--    Sem UNIQUE em cpf_cnpj → WHERE NOT EXISTS para idempotência
-- ============================================================

-- 4.1 PESSOA JURÍDICA (4)

INSERT INTO fornecedor (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    contato_nome, telefone, celular, email, site,
    cidade, uf, cep,
    banco_nome, banco_agencia, banco_conta, banco_tipo_conta,
    banco_pix_tipo, banco_pix_chave,
    ativo, criado_em, atualizado_em
)
SELECT
    1, 'PJ', 'Distribuidora Alimentos Brasil Ltda',
    'Distribuidora Alimentos Brasil Ltda', '11.222.333/0001-81',
    'Carlos Mendes', '(11) 3344-5566', '(11) 98877-6655',
    'carlos@alimentosbrasil.com.br', 'www.alimentosbrasil.com.br',
    'São Paulo', 'SP', '01310-100',
    'Itaú', '1234', '56789-0', 'CORRENTE',
    'CNPJ', '11222333000181',
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM fornecedor WHERE empresa_id = 1 AND cpf_cnpj = '11.222.333/0001-81'
);

INSERT INTO fornecedor (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    contato_nome, telefone, email,
    cidade, uf,
    banco_nome, banco_agencia, banco_conta, banco_tipo_conta,
    banco_pix_tipo, banco_pix_chave,
    ativo, criado_em, atualizado_em
)
SELECT
    1, 'PJ', 'Bebidas & Cia Distribuidora Ltda',
    'Bebidas e Cia Distribuidora Ltda', '34.028.316/0001-03',
    'Ana Souza', '(21) 2233-4455', 'ana@bebidasecia.com.br',
    'Rio de Janeiro', 'RJ',
    'Bradesco', '4567', '12345-6', 'CORRENTE',
    'EMAIL', 'financeiro@bebidasecia.com.br',
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM fornecedor WHERE empresa_id = 1 AND cpf_cnpj = '34.028.316/0001-03'
);

INSERT INTO fornecedor (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    contato_nome, telefone, email, site,
    cidade, uf,
    banco_pix_tipo, banco_pix_chave,
    ativo, criado_em, atualizado_em
)
SELECT
    1, 'PJ', 'TechSupply Eletrônicos Ltda',
    'TechSupply Eletronicos Ltda', '60.701.190/0001-04',
    'Roberto Lima', '(11) 4455-6677', 'roberto@techsupply.com.br',
    'www.techsupply.com.br',
    'São Paulo', 'SP',
    'CNPJ', '60701190000104',
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM fornecedor WHERE empresa_id = 1 AND cpf_cnpj = '60.701.190/0001-04'
);

INSERT INTO fornecedor (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    contato_nome, telefone, email,
    cidade, uf,
    ativo, criado_em, atualizado_em
)
SELECT
    1, 'PJ', 'Limpeza Total Distribuidora ME',
    'Limpeza Total Distribuidora ME', '07.526.557/0001-00',
    'Fernanda Costa', '(31) 3344-7788', 'fernanda@limpezatotal.com.br',
    'Belo Horizonte', 'MG',
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM fornecedor WHERE empresa_id = 1 AND cpf_cnpj = '07.526.557/0001-00'
);

-- 4.2 PESSOA FÍSICA (2)

INSERT INTO fornecedor (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    celular, email,
    cidade, uf,
    ativo, criado_em, atualizado_em
)
SELECT
    1, 'PF', 'João Pereira', '529.982.247-25',
    '(62) 99988-7766', 'joao.pereira@email.com',
    'Goiânia', 'GO',
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM fornecedor WHERE empresa_id = 1 AND cpf_cnpj = '529.982.247-25'
);

INSERT INTO fornecedor (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    celular,
    cidade, uf,
    ativo, criado_em, atualizado_em
)
SELECT
    1, 'PF', 'Maria Aparecida Santos', '295.669.480-52',
    '(41) 98866-5544',
    'Curitiba', 'PR',
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM fornecedor WHERE empresa_id = 1 AND cpf_cnpj = '295.669.480-52'
);


-- ============================================================
-- 5. CLIENTES (15 clientes)
--    Sem UNIQUE em cpf_cnpj → WHERE NOT EXISTS para idempotência
--    credito_disponivel = limite_credito (sem saldo usado)
-- ============================================================

-- 5.1 PESSOA FÍSICA (8)

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    telefone, celular, email,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PF', 'João da Silva', '131.798.736-09',
    '(11) 3333-4444', '(11) 98765-4321', 'joao.silva@email.com',
    'São Paulo', 'SP',
    1500.00, 1500.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '131.798.736-09'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    celular, email,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PF', 'Maria Oliveira', '098.219.830-90',
    '(21) 97654-3210', 'maria.oliveira@email.com',
    'Rio de Janeiro', 'RJ',
    800.00, 800.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '098.219.830-90'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    telefone, celular,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PF', 'Carlos Eduardo Santos', '038.596.510-04',
    '(31) 3211-5566', '(31) 99988-7766',
    'Belo Horizonte', 'MG',
    0.00, 0.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '038.596.510-04'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    celular, email,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PF', 'Ana Paula Ferreira', '770.667.890-04',
    '(41) 98877-6655', 'anapaula@email.com',
    'Curitiba', 'PR',
    2000.00, 2000.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '770.667.890-04'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    telefone,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PF', 'Roberto Carlos Mendes', '872.064.440-75',
    '(51) 3344-5566',
    'Porto Alegre', 'RS',
    500.00, 500.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '872.064.440-75'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    celular, email,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PF', 'Fernanda Lima', '652.545.330-00',
    '(61) 99877-5544', 'fernanda.lima@email.com',
    'Brasília', 'DF',
    1200.00, 1200.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '652.545.330-00'
);

-- Cliente INATIVO → para testar filtro de status
INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    celular,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PF', 'Paulo Henrique Costa', '111.444.777-35',
    '(85) 98866-4433',
    'Fortaleza', 'CE',
    0.00, 0.00,
    false, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '111.444.777-35'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, cpf_cnpj,
    email,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PF', 'Juliana Martins', '671.919.400-21',
    'juliana.martins@email.com',
    'Salvador', 'BA',
    600.00, 600.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '671.919.400-21'
);

-- 5.2 PESSOA JURÍDICA (7)

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    telefone, email,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PJ', 'Mercado do Bairro',
    'Mercado do Bairro Comércio Ltda', '19.521.298/0001-17',
    '(11) 4455-6677', 'compras@mercadobairro.com.br',
    'São Paulo', 'SP',
    5000.00, 5000.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '19.521.298/0001-17'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    telefone,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PJ', 'Restaurante Sabor Caseiro',
    'Restaurante Sabor Caseiro Ltda ME', '23.040.728/0001-90',
    '(11) 2233-4455',
    'São Paulo', 'SP',
    3000.00, 3000.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '23.040.728/0001-90'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    telefone,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PJ', 'Padaria Pão Quente',
    'Padaria Pao Quente Eireli', '45.723.174/0001-20',
    '(21) 3344-5566',
    'Rio de Janeiro', 'RJ',
    2500.00, 2500.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '45.723.174/0001-20'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    celular, email,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PJ', 'Loja da Maria',
    'Loja da Maria Comercio ME', '56.213.634/0001-80',
    '(31) 98855-4433', 'maria@lojadamaria.com.br',
    'Belo Horizonte', 'MG',
    1800.00, 1800.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '56.213.634/0001-80'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    telefone, email,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PJ', 'Distribuidora Norte',
    'Distribuidora Norte Ltda', '36.285.518/0001-08',
    '(91) 3344-6677', 'vendas@distribuidoranorte.com.br',
    'Belém', 'PA',
    8000.00, 8000.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '36.285.518/0001-08'
);

-- Cliente PJ INATIVO → para testar filtro de status
INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    telefone,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PJ', 'Açougue Central',
    'Acougue Central Comercio Ltda', '00.954.439/0001-58',
    '(62) 3344-5566',
    'Goiânia', 'GO',
    4000.00, 4000.00,
    false, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '00.954.439/0001-58'
);

INSERT INTO cliente (
    empresa_id, tipo_pessoa, nome, razao_social, cpf_cnpj,
    telefone, email,
    cidade, uf,
    limite_credito, credito_disponivel,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'PJ', 'Farmácia Saúde & Vida',
    'Farmacia Saude e Vida Ltda', '09.167.543/0001-05',
    '(51) 3322-4455', 'compras@saudaevida.com.br',
    'Porto Alegre', 'RS',
    6000.00, 6000.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '09.167.543/0001-05'
);


-- ============================================================
-- 6. FUNCIONÁRIOS (6 funcionários)
--    usuario_id = NULL (sem conta de acesso ao sistema)
--    Sem UNIQUE em cpf → WHERE NOT EXISTS para idempotência
-- ============================================================

INSERT INTO funcionario (
    empresa_id, nome, cpf, cargo, telefone, email,
    data_admissao, percentual_comissao,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'Pedro Alves', '417.535.580-20',
    'Vendedor', '(11) 98765-0001', 'pedro.alves@sms.com',
    '2024-03-01', 2.50,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM funcionario WHERE empresa_id = 1 AND cpf = '417.535.580-20'
);

INSERT INTO funcionario (
    empresa_id, nome, cpf, cargo, telefone, email,
    data_admissao, percentual_comissao,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'Camila Rodrigues', '060.775.540-20',
    'Caixa', '(11) 98765-0002', 'camila.rodrigues@sms.com',
    '2024-05-15', 0.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM funcionario WHERE empresa_id = 1 AND cpf = '060.775.540-20'
);

INSERT INTO funcionario (
    empresa_id, nome, cpf, cargo, telefone, email,
    data_admissao, percentual_comissao,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'Lucas Ferreira', '376.826.450-25',
    'Estoquista', '(11) 98765-0003', 'lucas.ferreira@sms.com',
    '2023-11-10', 0.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM funcionario WHERE empresa_id = 1 AND cpf = '376.826.450-25'
);

INSERT INTO funcionario (
    empresa_id, nome, cpf, cargo, telefone, email,
    data_admissao, percentual_comissao,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'Beatriz Santos', '247.555.930-58',
    'Gerente Comercial', '(11) 98765-0004', 'beatriz.santos@sms.com',
    '2023-08-01', 1.00,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM funcionario WHERE empresa_id = 1 AND cpf = '247.555.930-58'
);

INSERT INTO funcionario (
    empresa_id, nome, cpf, cargo, telefone,
    data_admissao, percentual_comissao,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'Marcos Oliveira', '059.381.390-40',
    'Vendedor', '(11) 98765-0005',
    '2024-01-15', 2.50,
    true, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM funcionario WHERE empresa_id = 1 AND cpf = '059.381.390-40'
);

-- Funcionária INATIVA → para testar filtro de status
INSERT INTO funcionario (
    empresa_id, nome, cpf, cargo,
    data_admissao, percentual_comissao,
    ativo, criado_em, atualizado_em
)
SELECT 1, 'Patrícia Lima', '705.548.090-18',
    'Auxiliar Administrativo',
    '2022-06-01', 0.00,
    false, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM funcionario WHERE empresa_id = 1 AND cpf = '705.548.090-18'
);
