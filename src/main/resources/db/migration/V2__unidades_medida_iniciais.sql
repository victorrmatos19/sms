-- ============================================================
-- V2: Unidades de medida padrão para a empresa inicial (id=1)
-- Necessário para o módulo de Produtos funcionar na Fase 1.
-- Novas empresas deverão cadastrar suas próprias unidades.
-- ============================================================

INSERT INTO unidade_medida (empresa_id, sigla, descricao) VALUES
    (1, 'UN',  'Unidade'),
    (1, 'CX',  'Caixa'),
    (1, 'PC',  'Peça'),
    (1, 'KG',  'Quilograma'),
    (1, 'G',   'Grama'),
    (1, 'L',   'Litro'),
    (1, 'ML',  'Mililitro'),
    (1, 'M',   'Metro'),
    (1, 'M2',  'Metro quadrado'),
    (1, 'M3',  'Metro cúbico'),
    (1, 'CM',  'Centímetro'),
    (1, 'PAR', 'Par'),
    (1, 'DZ',  'Dúzia'),
    (1, 'PCT', 'Pacote'),
    (1, 'FD',  'Fardo'),
    (1, 'RL',  'Rolo'),
    (1, 'SC',  'Saco'),
    (1, 'TB',  'Tambor'),
    (1, 'AMP', 'Ampola'),
    (1, 'CPS', 'Cápsula')
ON CONFLICT (empresa_id, sigla) DO NOTHING;
