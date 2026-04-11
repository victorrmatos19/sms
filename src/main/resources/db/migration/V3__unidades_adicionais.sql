-- V3: Adiciona unidades de medida complementares para empresa_id = 1
-- Unidades que faltaram na V2: Metro (MT) e Par (PR)

INSERT INTO unidade_medida (empresa_id, sigla, descricao)
VALUES
    (1, 'MT', 'Metro'),
    (1, 'PR', 'Par')
ON CONFLICT (empresa_id, sigla) DO NOTHING;
