package com.erp.model.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO com todos os dados necessários para o dashboard do perfil ADMINISTRADOR.
 */
public record DashboardAdminDTO(
        // Compras
        long comprasMes,
        BigDecimal valorComprasMes,
        long rascunhos,
        // Vendas
        long vendasHoje,
        BigDecimal valorVendasHoje,
        BigDecimal ticketMedioUltimos7Dias,
        // Contas a pagar
        long contasPagarHoje,
        BigDecimal valorContasPagarHoje,
        long contasPagarVencidas,
        long contasPagarAbertas,
        BigDecimal totalContasPagarAbertas,
        // Estoque
        long produtosAbaixoMinimo,
        long produtosEstoqueZerado,
        long totalProdutosAtivos,
        // Gráfico
        List<VendaDiariaDTO> vendasSemana,
        // Painéis
        List<AlertaDTO> alertas,
        List<TopProdutoDTO> topProdutos,
        List<VendaResumoDTO> vendasHojeDetalhes
) {}
