package com.erp.model.dto.relatorio;

import java.math.BigDecimal;

public record RelatorioEstoqueTotaisDTO(
        long totalProdutosAtivos,
        long produtosNormais,
        long produtosAbaixoMinimo,
        long produtosZeradosNegativos,
        BigDecimal valorTotalEstoque
) {
}
