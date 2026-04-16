package com.erp.model.dto.financeiro;

import java.math.BigDecimal;

public record ContasReceberResumoDTO(
        long abertas,
        BigDecimal totalAbertas,
        long vencidas,
        BigDecimal totalVencidas,
        long vencendoHoje,
        BigDecimal totalVencendoHoje,
        long recebidasMes,
        BigDecimal totalRecebidasMes
) {
}
