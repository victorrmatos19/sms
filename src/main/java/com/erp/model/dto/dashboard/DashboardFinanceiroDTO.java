package com.erp.model.dto.dashboard;

import java.math.BigDecimal;

/**
 * DTO com dados para o dashboard do perfil FINANCEIRO.
 */
public record DashboardFinanceiroDTO(
        long contasPagarAbertas,
        BigDecimal totalContasPagarAbertas,
        long contasPagarVencidas,
        BigDecimal totalContasPagarVencidas,
        long comprasMes,
        BigDecimal valorComprasMes
) {}
