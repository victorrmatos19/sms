package com.erp.model.dto.dashboard;

import java.math.BigDecimal;

/**
 * DTO com dados para o dashboard do perfil VENDAS.
 */
public record DashboardVendasDTO(
        long minhasVendasHoje,
        BigDecimal valorMinhasVendasHoje,
        long minhasVendasMes,
        BigDecimal valorMinhasVendasMes,
        long meusOrcamentosAbertos,
        long meusOrcamentosMes,
        BigDecimal valorMeusOrcamentosMes,
        long meusOrcamentosConvertidosMes,
        long vendasTimeMes,
        long orcamentosTimeMes
) {}
