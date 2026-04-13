package com.erp.model.dto.dashboard;

/**
 * DTO com dados para o dashboard do perfil ESTOQUE.
 */
public record DashboardEstoqueDTO(
        long totalProdutosAtivos,
        long produtosAbaixoMinimo,
        long entradasMes,
        long saidasMes
) {}
