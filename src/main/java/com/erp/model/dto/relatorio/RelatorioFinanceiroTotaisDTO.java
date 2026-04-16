package com.erp.model.dto.relatorio;

import java.math.BigDecimal;

public record RelatorioFinanceiroTotaisDTO(
        BigDecimal pagarAberto,
        BigDecimal pagarVencido,
        BigDecimal pagarPagoPeriodo,
        BigDecimal receberAberto,
        BigDecimal receberVencido,
        BigDecimal receberRecebidoPeriodo
) {
}
