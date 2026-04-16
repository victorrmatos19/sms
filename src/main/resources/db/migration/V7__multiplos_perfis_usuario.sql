-- V7: múltiplos perfis por usuário.
-- A coluna usuario.perfil_id fica como legado compatível; a fonte de verdade
-- passa a ser usuario_perfil.

CREATE TABLE IF NOT EXISTS usuario_perfil (
    usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    perfil_id  INTEGER NOT NULL REFERENCES perfil_acesso(id),
    PRIMARY KEY (usuario_id, perfil_id)
);

INSERT INTO usuario_perfil (usuario_id, perfil_id)
SELECT id, perfil_id
FROM usuario
WHERE perfil_id IS NOT NULL
ON CONFLICT DO NOTHING;

ALTER TABLE usuario ALTER COLUMN perfil_id DROP NOT NULL;
