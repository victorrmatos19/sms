package com.erp.model.dto.financeiro;

import java.math.BigDecimal;

public record ContasPagarResumoDTO(
        long abertas,
        BigDecimal totalAbertas,
        long vencidas,
        BigDecimal totalVencidas,
        long vencendoHoje,
        BigDecimal totalVencendoHoje,
        long pagasMes,
        BigDecimal totalPagasMes
) {
}
