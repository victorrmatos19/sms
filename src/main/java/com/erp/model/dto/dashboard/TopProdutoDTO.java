package com.erp.model.dto.dashboard;

import java.math.BigDecimal;

/**
 * Produto mais comprado no mês para o ranking do dashboard.
 */
public record TopProdutoDTO(String nome, BigDecimal valor) {}
