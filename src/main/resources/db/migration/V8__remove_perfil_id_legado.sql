-- Remove coluna perfil_id legada da tabela usuario.
-- A fonte de verdade para perfis é a tabela usuario_perfil (criada em V7).
-- Todos os dados foram migrados para usuario_perfil na migration V7.

ALTER TABLE usuario DROP COLUMN IF EXISTS perfil_id;
