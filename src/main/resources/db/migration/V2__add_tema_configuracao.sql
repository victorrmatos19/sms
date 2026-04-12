-- Migration V2: Adiciona coluna 'tema' na tabela configuracao
-- Suporta os valores 'LIGHT' e 'DARK'. Padrão: 'DARK'.

ALTER TABLE configuracao ADD COLUMN IF NOT EXISTS
    tema VARCHAR(10) NOT NULL DEFAULT 'DARK';

UPDATE configuracao SET tema = 'DARK' WHERE tema IS NULL;
