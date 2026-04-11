-- ============================================================
-- ERP Desktop — Fase 1: Core Comercial
-- Migration: V1__schema_fase1.sql
-- Flyway executa este arquivo automaticamente na primeira inicialização
-- ============================================================

-- ============================================================
-- EXTENSÕES
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "unaccent";

-- Wrapper IMMUTABLE para unaccent — necessário para uso em índices GIN.
-- O unaccent padrão é STABLE, mas índices exigem IMMUTABLE.
CREATE OR REPLACE FUNCTION unaccent_immutable(text)
    RETURNS text
    LANGUAGE sql
    IMMUTABLE
    PARALLEL SAFE
    STRICT
AS $$
SELECT unaccent($1);
$$;


-- ============================================================
-- 1. EMPRESA E CONFIGURAÇÕES
-- ============================================================

CREATE TABLE empresa (
                         id                  SERIAL PRIMARY KEY,
                         razao_social        VARCHAR(150) NOT NULL,
                         nome_fantasia       VARCHAR(150),
                         cnpj                VARCHAR(18)  NOT NULL UNIQUE,
                         inscricao_estadual  VARCHAR(30),
                         inscricao_municipal VARCHAR(30),
                         regime_tributario   VARCHAR(30)  NOT NULL,
                         logradouro          VARCHAR(150),
                         numero              VARCHAR(20),
                         complemento         VARCHAR(80),
                         bairro              VARCHAR(80),
                         cidade              VARCHAR(80),
                         uf                  VARCHAR(2),
                         cep                 VARCHAR(9),
                         telefone            VARCHAR(20),
                         email               VARCHAR(120),
                         site                VARCHAR(120),
                         logotipo            BYTEA,
                         criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
                         atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE configuracao (
                              id                          SERIAL PRIMARY KEY,
                              empresa_id                  INTEGER NOT NULL REFERENCES empresa(id),
                              usa_lote_validade           BOOLEAN NOT NULL DEFAULT FALSE,
                              alerta_estoque_minimo       BOOLEAN NOT NULL DEFAULT TRUE,
                              permite_venda_estoque_zero  BOOLEAN NOT NULL DEFAULT FALSE,
                              dias_validade_orcamento     INTEGER NOT NULL DEFAULT 30,
                              juros_mora_padrao           NUMERIC(5,2) NOT NULL DEFAULT 0.00,
                              multa_padrao                NUMERIC(5,2) NOT NULL DEFAULT 0.00,
                              impressora_padrao           VARCHAR(100),
                              exibir_logotipo_impressao   BOOLEAN NOT NULL DEFAULT TRUE,
                              criado_em                   TIMESTAMP NOT NULL DEFAULT NOW(),
                              atualizado_em               TIMESTAMP NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 2. USUÁRIOS E PERFIS DE ACESSO
-- ============================================================

CREATE TABLE perfil_acesso (
                               id          SERIAL PRIMARY KEY,
                               nome        VARCHAR(50) NOT NULL UNIQUE,
                               descricao   VARCHAR(200)
);

CREATE TABLE usuario (
                         id              SERIAL PRIMARY KEY,
                         empresa_id      INTEGER      NOT NULL REFERENCES empresa(id),
                         perfil_id       INTEGER      NOT NULL REFERENCES perfil_acesso(id),
                         nome            VARCHAR(100) NOT NULL,
                         login           VARCHAR(50)  NOT NULL,
                         senha_hash      VARCHAR(255) NOT NULL,
                         email           VARCHAR(120),
                         ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
                         ultimo_acesso   TIMESTAMP,
                         criado_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
                         atualizado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
                         UNIQUE (empresa_id, login)
);


-- ============================================================
-- 3. CADASTRO DE FUNCIONÁRIOS
-- ============================================================

CREATE TABLE funcionario (
                             id                   SERIAL PRIMARY KEY,
                             empresa_id           INTEGER      NOT NULL REFERENCES empresa(id),
                             usuario_id           INTEGER      REFERENCES usuario(id),
                             nome                 VARCHAR(100) NOT NULL,
                             cpf                  VARCHAR(14),
                             cargo                VARCHAR(80),
                             telefone             VARCHAR(20),
                             email                VARCHAR(120),
                             data_admissao        DATE,
                             ativo                BOOLEAN      NOT NULL DEFAULT TRUE,
                             percentual_comissao  NUMERIC(5,2) NOT NULL DEFAULT 0.00,
                             criado_em            TIMESTAMP    NOT NULL DEFAULT NOW(),
                             atualizado_em        TIMESTAMP    NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 4. CLIENTES
-- ============================================================

CREATE TABLE cliente (
                         id                  SERIAL PRIMARY KEY,
                         empresa_id          INTEGER       NOT NULL REFERENCES empresa(id),
                         tipo_pessoa         VARCHAR(2)    NOT NULL,
                         nome                VARCHAR(150)  NOT NULL,
                         razao_social        VARCHAR(150),
                         cpf_cnpj            VARCHAR(18),
                         rg_ie               VARCHAR(30),
                         logradouro          VARCHAR(150),
                         numero              VARCHAR(20),
                         complemento         VARCHAR(80),
                         bairro              VARCHAR(80),
                         cidade              VARCHAR(80),
                         uf                  VARCHAR(2),
                         cep                 VARCHAR(9),
                         telefone            VARCHAR(20),
                         celular             VARCHAR(20),
                         email               VARCHAR(120),
                         limite_credito      NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                         credito_disponivel  NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                         observacoes         TEXT,
                         ativo               BOOLEAN       NOT NULL DEFAULT TRUE,
                         criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
                         atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 5. FORNECEDORES
-- ============================================================

CREATE TABLE fornecedor (
                            id                  SERIAL PRIMARY KEY,
                            empresa_id          INTEGER      NOT NULL REFERENCES empresa(id),
                            tipo_pessoa         VARCHAR(2)   NOT NULL,
                            nome                VARCHAR(150) NOT NULL,
                            razao_social        VARCHAR(150),
                            cpf_cnpj            VARCHAR(18),
                            inscricao_estadual  VARCHAR(30),
                            logradouro          VARCHAR(150),
                            numero              VARCHAR(20),
                            complemento         VARCHAR(80),
                            bairro              VARCHAR(80),
                            cidade              VARCHAR(80),
                            uf                  VARCHAR(2),
                            cep                 VARCHAR(9),
                            telefone            VARCHAR(20),
                            celular             VARCHAR(20),
                            email               VARCHAR(120),
                            site                VARCHAR(120),
                            contato_nome        VARCHAR(100),
                            banco_nome          VARCHAR(60),
                            banco_agencia       VARCHAR(10),
                            banco_conta         VARCHAR(20),
                            banco_tipo_conta    VARCHAR(20),
                            banco_pix_chave     VARCHAR(150),
                            banco_pix_tipo      VARCHAR(20),
                            observacoes         TEXT,
                            ativo               BOOLEAN   NOT NULL DEFAULT TRUE,
                            criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
                            atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 6. PRODUTOS E ESTOQUE
-- ============================================================

CREATE TABLE grupo_produto (
                               id          SERIAL PRIMARY KEY,
                               empresa_id  INTEGER     NOT NULL REFERENCES empresa(id),
                               nome        VARCHAR(80) NOT NULL,
                               descricao   VARCHAR(200),
                               ativo       BOOLEAN     NOT NULL DEFAULT TRUE,
                               UNIQUE (empresa_id, nome)
);

CREATE TABLE categoria_produto (
                                   id          SERIAL PRIMARY KEY,
                                   empresa_id  INTEGER     NOT NULL REFERENCES empresa(id),
                                   grupo_id    INTEGER     REFERENCES grupo_produto(id),
                                   nome        VARCHAR(80) NOT NULL,
                                   descricao   VARCHAR(200),
                                   ativo       BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE unidade_medida (
                                id          SERIAL PRIMARY KEY,
                                empresa_id  INTEGER     NOT NULL REFERENCES empresa(id),
                                sigla       VARCHAR(10) NOT NULL,
                                descricao   VARCHAR(60),
                                UNIQUE (empresa_id, sigla)
);

CREATE TABLE produto (
                         id                  SERIAL PRIMARY KEY,
                         empresa_id          INTEGER       NOT NULL REFERENCES empresa(id),
                         grupo_id            INTEGER       REFERENCES grupo_produto(id),
                         categoria_id        INTEGER       REFERENCES categoria_produto(id),
                         unidade_id          INTEGER       REFERENCES unidade_medida(id),
                         codigo_interno      VARCHAR(30),
                         codigo_barras       VARCHAR(60),
                         descricao           VARCHAR(200)  NOT NULL,
                         descricao_reduzida  VARCHAR(60),
                         preco_custo         NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
                         margem_lucro        NUMERIC(7,4)  NOT NULL DEFAULT 0.0000,
                         preco_venda         NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
                         preco_minimo        NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
                         estoque_atual       NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
                         estoque_minimo      NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
                         estoque_maximo      NUMERIC(15,4),
                         localizacao         VARCHAR(60),
                         ncm                 VARCHAR(10),
                         cest                VARCHAR(10),
                         origem              VARCHAR(1),
                         usa_lote_validade   BOOLEAN       NOT NULL DEFAULT FALSE,
                         ativo               BOOLEAN       NOT NULL DEFAULT TRUE,
                         observacoes         TEXT,
                         criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
                         atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
                         UNIQUE (empresa_id, codigo_interno),
                         UNIQUE (empresa_id, codigo_barras)
);

CREATE TABLE produto_codigo_barras (
                                       id          SERIAL PRIMARY KEY,
                                       produto_id  INTEGER      NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
                                       codigo      VARCHAR(60)  NOT NULL,
                                       descricao   VARCHAR(80),
                                       fator       NUMERIC(10,4) NOT NULL DEFAULT 1.0000,
                                       UNIQUE (produto_id, codigo)
);

CREATE TABLE produto_fornecedor (
                                    id                  SERIAL PRIMARY KEY,
                                    produto_id          INTEGER       NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
                                    fornecedor_id       INTEGER       NOT NULL REFERENCES fornecedor(id),
                                    codigo_fornecedor   VARCHAR(60),
                                    preco_custo         NUMERIC(15,4),
                                    prazo_entrega_dias  INTEGER,
                                    preferencial        BOOLEAN       NOT NULL DEFAULT FALSE,
                                    UNIQUE (produto_id, fornecedor_id)
);


-- ============================================================
-- 7. LOTE E VALIDADE
-- ============================================================

CREATE TABLE lote (
                      id              SERIAL PRIMARY KEY,
                      produto_id      INTEGER       NOT NULL REFERENCES produto(id),
                      numero_lote     VARCHAR(60)   NOT NULL,
                      data_fabricacao DATE,
                      data_validade   DATE,
                      quantidade      NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
                      observacoes     VARCHAR(200),
                      criado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
                      UNIQUE (produto_id, numero_lote)
);


-- ============================================================
-- 8. MOVIMENTAÇÕES DE ESTOQUE
-- ============================================================

CREATE TABLE movimentacao_estoque (
                                      id              SERIAL PRIMARY KEY,
                                      empresa_id      INTEGER       NOT NULL REFERENCES empresa(id),
                                      produto_id      INTEGER       NOT NULL REFERENCES produto(id),
                                      lote_id         INTEGER       REFERENCES lote(id),
                                      tipo            VARCHAR(20)   NOT NULL,
                                      origem          VARCHAR(30)   NOT NULL,
                                      origem_id       INTEGER,
                                      quantidade      NUMERIC(15,4) NOT NULL,
                                      custo_unitario  NUMERIC(15,4),
                                      saldo_anterior  NUMERIC(15,4) NOT NULL,
                                      saldo_posterior NUMERIC(15,4) NOT NULL,
                                      observacoes     VARCHAR(200),
                                      usuario_id      INTEGER       REFERENCES usuario(id),
                                      criado_em       TIMESTAMP     NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 9. COMPRAS
-- ============================================================

CREATE TABLE compra (
                        id                  SERIAL PRIMARY KEY,
                        empresa_id          INTEGER       NOT NULL REFERENCES empresa(id),
                        fornecedor_id       INTEGER       NOT NULL REFERENCES fornecedor(id),
                        usuario_id          INTEGER       REFERENCES usuario(id),
                        numero_documento    VARCHAR(60),
                        status              VARCHAR(20)   NOT NULL DEFAULT 'RASCUNHO',
                        data_emissao        DATE          NOT NULL DEFAULT CURRENT_DATE,
                        data_previsao       DATE,
                        data_recebimento    DATE,
                        valor_produtos      NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                        valor_frete         NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                        valor_desconto      NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                        valor_outras        NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                        valor_total         NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                        condicao_pagamento  VARCHAR(30),
                        observacoes         TEXT,
                        criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
                        atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE compra_item (
                             id              SERIAL PRIMARY KEY,
                             compra_id       INTEGER       NOT NULL REFERENCES compra(id) ON DELETE CASCADE,
                             produto_id      INTEGER       NOT NULL REFERENCES produto(id),
                             lote_id         INTEGER       REFERENCES lote(id),
                             quantidade      NUMERIC(15,4) NOT NULL,
                             custo_unitario  NUMERIC(15,4) NOT NULL,
                             desconto        NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                             valor_total     NUMERIC(15,2) NOT NULL
);


-- ============================================================
-- 10. ORÇAMENTOS
-- ============================================================

CREATE TABLE orcamento (
                           id                  SERIAL PRIMARY KEY,
                           empresa_id          INTEGER       NOT NULL REFERENCES empresa(id),
                           cliente_id          INTEGER       REFERENCES cliente(id),
                           vendedor_id         INTEGER       REFERENCES funcionario(id),
                           usuario_id          INTEGER       REFERENCES usuario(id),
                           numero              VARCHAR(20)   NOT NULL,
                           status              VARCHAR(20)   NOT NULL DEFAULT 'ABERTO',
                           data_emissao        DATE          NOT NULL DEFAULT CURRENT_DATE,
                           data_validade       DATE          NOT NULL,
                           valor_produtos      NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                           valor_desconto      NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                           valor_total         NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                           observacoes         TEXT,
                           criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
                           atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
                           UNIQUE (empresa_id, numero)
);

CREATE TABLE orcamento_item (
                                id              SERIAL PRIMARY KEY,
                                orcamento_id    INTEGER       NOT NULL REFERENCES orcamento(id) ON DELETE CASCADE,
                                produto_id      INTEGER       NOT NULL REFERENCES produto(id),
                                descricao       VARCHAR(200)  NOT NULL,
                                quantidade      NUMERIC(15,4) NOT NULL,
                                preco_unitario  NUMERIC(15,4) NOT NULL,
                                desconto        NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                                valor_total     NUMERIC(15,2) NOT NULL
);


-- ============================================================
-- 11. CAIXA
-- ============================================================

CREATE TABLE caixa (
                       id          SERIAL PRIMARY KEY,
                       empresa_id  INTEGER     NOT NULL REFERENCES empresa(id),
                       nome        VARCHAR(60) NOT NULL,
                       ativo       BOOLEAN     NOT NULL DEFAULT TRUE,
                       UNIQUE (empresa_id, nome)
);

CREATE TABLE caixa_sessao (
                              id                      SERIAL PRIMARY KEY,
                              caixa_id                INTEGER       NOT NULL REFERENCES caixa(id),
                              usuario_id              INTEGER       NOT NULL REFERENCES usuario(id),
                              status                  VARCHAR(20)   NOT NULL DEFAULT 'ABERTO',
                              data_abertura           TIMESTAMP     NOT NULL DEFAULT NOW(),
                              data_fechamento         TIMESTAMP,
                              saldo_inicial           NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                              saldo_final_informado   NUMERIC(15,2),
                              saldo_final_calculado   NUMERIC(15,2),
                              diferenca               NUMERIC(15,2),
                              observacoes             TEXT
);

CREATE TABLE caixa_movimentacao (
                                    id              SERIAL PRIMARY KEY,
                                    sessao_id       INTEGER       NOT NULL REFERENCES caixa_sessao(id),
                                    tipo            VARCHAR(20)   NOT NULL,
                                    descricao       VARCHAR(150)  NOT NULL,
                                    valor           NUMERIC(15,2) NOT NULL,
                                    forma_pagamento VARCHAR(30),
                                    origem          VARCHAR(30),
                                    origem_id       INTEGER,
                                    usuario_id      INTEGER       REFERENCES usuario(id),
                                    criado_em       TIMESTAMP     NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 12. VENDAS
-- ============================================================

CREATE TABLE venda (
                       id                  SERIAL PRIMARY KEY,
                       empresa_id          INTEGER       NOT NULL REFERENCES empresa(id),
                       cliente_id          INTEGER       REFERENCES cliente(id),
                       vendedor_id         INTEGER       REFERENCES funcionario(id),
                       usuario_id          INTEGER       REFERENCES usuario(id),
                       caixa_id            INTEGER       REFERENCES caixa_sessao(id),
                       orcamento_id        INTEGER       REFERENCES orcamento(id),
                       numero              VARCHAR(20)   NOT NULL,
                       status              VARCHAR(20)   NOT NULL DEFAULT 'FINALIZADA',
                       data_venda          TIMESTAMP     NOT NULL DEFAULT NOW(),
                       valor_produtos      NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                       valor_desconto      NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                       valor_total         NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                       percentual_comissao NUMERIC(5,2)  NOT NULL DEFAULT 0.00,
                       valor_comissao      NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                       observacoes         TEXT,
                       criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
                       atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW(),
                       UNIQUE (empresa_id, numero)
);

CREATE TABLE venda_item (
                            id              SERIAL PRIMARY KEY,
                            venda_id        INTEGER       NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
                            produto_id      INTEGER       NOT NULL REFERENCES produto(id),
                            lote_id         INTEGER       REFERENCES lote(id),
                            descricao       VARCHAR(200)  NOT NULL,
                            quantidade      NUMERIC(15,4) NOT NULL,
                            preco_unitario  NUMERIC(15,4) NOT NULL,
                            custo_unitario  NUMERIC(15,4) NOT NULL DEFAULT 0.0000,
                            desconto        NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                            valor_total     NUMERIC(15,2) NOT NULL
);

CREATE TABLE venda_pagamento (
                                 id              SERIAL PRIMARY KEY,
                                 venda_id        INTEGER       NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
                                 forma_pagamento VARCHAR(30)   NOT NULL,
                                 valor           NUMERIC(15,2) NOT NULL,
                                 parcelas        INTEGER       NOT NULL DEFAULT 1,
                                 observacoes     VARCHAR(100)
);


-- ============================================================
-- 13. CONTAS A RECEBER
-- ============================================================

CREATE TABLE conta_receber (
                               id                  SERIAL PRIMARY KEY,
                               empresa_id          INTEGER       NOT NULL REFERENCES empresa(id),
                               cliente_id          INTEGER       REFERENCES cliente(id),
                               venda_id            INTEGER       REFERENCES venda(id),
                               descricao           VARCHAR(200)  NOT NULL,
                               numero_parcela      INTEGER       NOT NULL DEFAULT 1,
                               total_parcelas      INTEGER       NOT NULL DEFAULT 1,
                               valor               NUMERIC(15,2) NOT NULL,
                               valor_pago          NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                               juros               NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                               multa               NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                               desconto            NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                               data_emissao        DATE          NOT NULL DEFAULT CURRENT_DATE,
                               data_vencimento     DATE          NOT NULL,
                               data_recebimento    DATE,
                               status              VARCHAR(20)   NOT NULL DEFAULT 'ABERTA',
                               forma_recebimento   VARCHAR(30),
                               observacoes         TEXT,
                               usuario_id          INTEGER       REFERENCES usuario(id),
                               criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
                               atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 14. CONTAS A PAGAR
-- ============================================================

CREATE TABLE conta_pagar (
                             id                  SERIAL PRIMARY KEY,
                             empresa_id          INTEGER       NOT NULL REFERENCES empresa(id),
                             fornecedor_id       INTEGER       REFERENCES fornecedor(id),
                             compra_id           INTEGER       REFERENCES compra(id),
                             descricao           VARCHAR(200)  NOT NULL,
                             numero_parcela      INTEGER       NOT NULL DEFAULT 1,
                             total_parcelas      INTEGER       NOT NULL DEFAULT 1,
                             valor               NUMERIC(15,2) NOT NULL,
                             valor_pago          NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                             juros               NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                             multa               NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                             desconto            NUMERIC(15,2) NOT NULL DEFAULT 0.00,
                             data_emissao        DATE          NOT NULL DEFAULT CURRENT_DATE,
                             data_vencimento     DATE          NOT NULL,
                             data_pagamento      DATE,
                             status              VARCHAR(20)   NOT NULL DEFAULT 'ABERTA',
                             forma_pagamento     VARCHAR(30),
                             observacoes         TEXT,
                             usuario_id          INTEGER       REFERENCES usuario(id),
                             criado_em           TIMESTAMP     NOT NULL DEFAULT NOW(),
                             atualizado_em       TIMESTAMP     NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 15. ÍNDICES DE PERFORMANCE
-- ============================================================

CREATE INDEX idx_produto_empresa        ON produto(empresa_id);
CREATE INDEX idx_produto_codigo_barras  ON produto(codigo_barras);
CREATE INDEX idx_produto_descricao      ON produto USING gin(to_tsvector('portuguese', unaccent_immutable(descricao)));

CREATE INDEX idx_cliente_empresa        ON cliente(empresa_id);
CREATE INDEX idx_cliente_cpf_cnpj       ON cliente(cpf_cnpj);
CREATE INDEX idx_cliente_nome           ON cliente USING gin(to_tsvector('portuguese', unaccent_immutable(nome)));

CREATE INDEX idx_fornecedor_empresa     ON fornecedor(empresa_id);
CREATE INDEX idx_fornecedor_cpf_cnpj    ON fornecedor(cpf_cnpj);

CREATE INDEX idx_venda_empresa          ON venda(empresa_id);
CREATE INDEX idx_venda_cliente          ON venda(cliente_id);
CREATE INDEX idx_venda_vendedor         ON venda(vendedor_id);
CREATE INDEX idx_venda_data             ON venda(data_venda);
CREATE INDEX idx_venda_status           ON venda(status);

CREATE INDEX idx_compra_empresa         ON compra(empresa_id);
CREATE INDEX idx_compra_fornecedor      ON compra(fornecedor_id);
CREATE INDEX idx_compra_status          ON compra(status);

CREATE INDEX idx_orcamento_empresa      ON orcamento(empresa_id);
CREATE INDEX idx_orcamento_cliente      ON orcamento(cliente_id);
CREATE INDEX idx_orcamento_status       ON orcamento(status);

CREATE INDEX idx_receber_empresa        ON conta_receber(empresa_id);
CREATE INDEX idx_receber_cliente        ON conta_receber(cliente_id);
CREATE INDEX idx_receber_vencimento     ON conta_receber(data_vencimento);
CREATE INDEX idx_receber_status         ON conta_receber(status);

CREATE INDEX idx_pagar_empresa          ON conta_pagar(empresa_id);
CREATE INDEX idx_pagar_fornecedor       ON conta_pagar(fornecedor_id);
CREATE INDEX idx_pagar_vencimento       ON conta_pagar(data_vencimento);
CREATE INDEX idx_pagar_status           ON conta_pagar(status);

CREATE INDEX idx_mov_estoque_produto    ON movimentacao_estoque(produto_id);
CREATE INDEX idx_mov_estoque_empresa    ON movimentacao_estoque(empresa_id);
CREATE INDEX idx_mov_estoque_data       ON movimentacao_estoque(criado_em);

CREATE INDEX idx_caixa_sessao_caixa     ON caixa_sessao(caixa_id);
CREATE INDEX idx_caixa_sessao_status    ON caixa_sessao(status);
CREATE INDEX idx_caixa_mov_sessao       ON caixa_movimentacao(sessao_id);


-- ============================================================
-- 16. DADOS INICIAIS
-- ============================================================

INSERT INTO perfil_acesso (nome, descricao) VALUES
                                                ('ADMINISTRADOR', 'Acesso total ao sistema'),
                                                ('GERENTE',       'Acesso total exceto configurações do sistema'),
                                                ('VENDAS',        'Orçamentos, vendas, clientes e consulta de estoque'),
                                                ('FINANCEIRO',    'Contas a pagar/receber, caixa e relatórios financeiros'),
                                                ('ESTOQUE',       'Produtos, fornecedores, entradas e saídas de estoque');

-- Empresa padrão
INSERT INTO empresa (razao_social, nome_fantasia, cnpj, regime_tributario)
VALUES ('Minha Empresa Ltda', 'Minha Empresa', '00.000.000/0001-00', 'SIMPLES_NACIONAL');

-- Usuário administrador padrão — senha: admin123
INSERT INTO usuario (empresa_id, perfil_id, nome, login, senha_hash, ativo)
VALUES (
           1,
           (SELECT id FROM perfil_acesso WHERE nome = 'ADMINISTRADOR'),
           'Administrador',
           'admin',
           '$2a$10$Vn0lZc73XD9DHdsPQ2zJOOIqQ.EUdFjZZGfqeP.j8y7m1OsFRAZO6',
           true
       );