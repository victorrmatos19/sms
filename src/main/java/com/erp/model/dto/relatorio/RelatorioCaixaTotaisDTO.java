package com.erp.model.dto.relatorio;

import java.math.BigDecimal;

public record RelatorioCaixaTotaisDTO(
        long totalSessoes,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldoLiquido
) {
}
