-- Remove tabela categoria_produto e a referência em produto.
-- O sistema usa apenas grupo_produto para classificação de produtos.

ALTER TABLE produto DROP COLUMN IF EXISTS categoria_id;
DROP TABLE IF EXISTS categoria_produto CASCADE;
