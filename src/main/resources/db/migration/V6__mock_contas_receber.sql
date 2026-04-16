-- ============================================================
-- V6: Dados mock para o módulo de Contas a Receber
-- empresa_id = 1
-- Idempotente: WHERE NOT EXISTS por empresa+descrição+data_vencimento
-- ============================================================

-- Datas relativas à data de execução do script
-- Usamos NOW() e offsets para simular realidade

-- ============================================================
-- CONTAS ABERTAS (vencimento futuro)
-- ============================================================

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '131.798.736-09'),
    'Venda a prazo — João da Silva 1/3',
    1, 3,
    450.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 10, CURRENT_DATE + 20,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Venda a prazo — João da Silva 1/3'
      AND data_vencimento = CURRENT_DATE + 20
);

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '131.798.736-09'),
    'Venda a prazo — João da Silva 2/3',
    2, 3,
    450.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 10, CURRENT_DATE + 50,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Venda a prazo — João da Silva 2/3'
      AND data_vencimento = CURRENT_DATE + 50
);

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '131.798.736-09'),
    'Venda a prazo — João da Silva 3/3',
    3, 3,
    450.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 10, CURRENT_DATE + 80,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Venda a prazo — João da Silva 3/3'
      AND data_vencimento = CURRENT_DATE + 80
);

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '19.521.298/0001-17'),
    'Crediário — Mercado do Bairro 1/2',
    1, 2,
    1200.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 5, CURRENT_DATE + 25,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Crediário — Mercado do Bairro 1/2'
      AND data_vencimento = CURRENT_DATE + 25
);

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '19.521.298/0001-17'),
    'Crediário — Mercado do Bairro 2/2',
    2, 2,
    1200.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 5, CURRENT_DATE + 55,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Crediário — Mercado do Bairro 2/2'
      AND data_vencimento = CURRENT_DATE + 55
);

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '098.219.830-90'),
    'Receita avulsa — Aluguel de espaço',
    1, 1,
    350.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE, CURRENT_DATE + 15,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Receita avulsa — Aluguel de espaço'
      AND data_vencimento = CURRENT_DATE + 15
);

-- ============================================================
-- CONTAS VENCENDO HOJE
-- ============================================================

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '23.040.728/0001-90'),
    'Crediário — Restaurante Sabor Caseiro 1/1',
    1, 1,
    780.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 30, CURRENT_DATE,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Crediário — Restaurante Sabor Caseiro 1/1'
      AND data_vencimento = CURRENT_DATE
);

-- ============================================================
-- CONTAS VENCIDAS (vencimento passado, status ABERTA)
-- ============================================================

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '770.667.890-04'),
    'Venda a prazo — Ana Paula Ferreira 1/2',
    1, 2,
    620.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 45, CURRENT_DATE - 15,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Venda a prazo — Ana Paula Ferreira 1/2'
      AND data_vencimento = CURRENT_DATE - 15
);

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '36.285.518/0001-08'),
    'Crediário — Distribuidora Norte 1/3',
    1, 3,
    2500.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 60, CURRENT_DATE - 30,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Crediário — Distribuidora Norte 1/3'
      AND data_vencimento = CURRENT_DATE - 30
);

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '652.545.330-00'),
    'Venda a prazo — Fernanda Lima 1/1',
    1, 1,
    180.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 50, CURRENT_DATE - 20,
    'ABERTA', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Venda a prazo — Fernanda Lima 1/1'
      AND data_vencimento = CURRENT_DATE - 20
);

-- ============================================================
-- CONTAS RECEBIDAS
-- ============================================================

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento, data_recebimento,
    status, forma_recebimento,
    criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '45.723.174/0001-20'),
    'Crediário — Padaria Pão Quente 1/2',
    1, 2,
    950.00, 950.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 60, CURRENT_DATE - 30, CURRENT_DATE - 28,
    'RECEBIDA', 'PIX',
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Crediário — Padaria Pão Quente 1/2'
      AND data_vencimento = CURRENT_DATE - 30
);

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento, data_recebimento,
    status, forma_recebimento,
    criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '56.213.634/0001-80'),
    'Venda a prazo — Loja da Maria 1/1',
    1, 1,
    320.00, 320.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 40, CURRENT_DATE - 10, CURRENT_DATE - 10,
    'RECEBIDA', 'DINHEIRO',
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Venda a prazo — Loja da Maria 1/1'
      AND data_vencimento = CURRENT_DATE - 10
);

INSERT INTO conta_receber (
    empresa_id, cliente_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento, data_recebimento,
    status, forma_recebimento,
    criado_em, atualizado_em
)
SELECT
    1,
    (SELECT id FROM cliente WHERE empresa_id = 1 AND cpf_cnpj = '09.167.543/0001-05'),
    'Receita avulsa — Farmácia Saúde e Vida',
    1, 1,
    75.00, 75.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 15, CURRENT_DATE - 5, CURRENT_DATE - 3,
    'RECEBIDA', 'CARTAO_DEBITO',
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Receita avulsa — Farmácia Saúde e Vida'
      AND data_vencimento = CURRENT_DATE - 5
);

-- ============================================================
-- CONTA CANCELADA
-- ============================================================

INSERT INTO conta_receber (
    empresa_id, descricao,
    numero_parcela, total_parcelas,
    valor, valor_pago, juros, multa, desconto,
    data_emissao, data_vencimento,
    status, observacoes,
    criado_em, atualizado_em
)
SELECT
    1,
    'Receita avulsa — Serviço cancelado',
    1, 1,
    500.00, 0.00, 0.00, 0.00, 0.00,
    CURRENT_DATE - 20, CURRENT_DATE - 10,
    'CANCELADA', 'Serviço não prestado — contrato cancelado',
    NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM conta_receber
    WHERE empresa_id = 1
      AND descricao = 'Receita avulsa — Serviço cancelado'
      AND data_vencimento = CURRENT_DATE - 10
);
