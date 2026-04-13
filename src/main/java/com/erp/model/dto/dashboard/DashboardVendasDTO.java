package com.erp.model.dto.dashboard;

import java.math.BigDecimal;

/**
 * DTO com dados para o dashboard do perfil VENDAS.
 * Orçamentos e Vendas ainda não implementados — valores zerados por padrão.
 */
public record DashboardVendasDTO(
        long orcamentosMes,
        BigDecimal valorOrcamentosMes,
        long vendasMes,
        BigDecimal valorVendasMes,
        long comprasMes,
        BigDecimal valorComprasMes
) {}
