package com.erp.model.dto.relatorio;

import java.math.BigDecimal;

public record RelatorioVendasTotaisDTO(
        long quantidadeVendas,
        BigDecimal valorBruto,
        BigDecimal totalDescontos,
        BigDecimal valorLiquido
) {
}
