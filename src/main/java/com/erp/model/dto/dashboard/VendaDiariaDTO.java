package com.erp.model.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa o total de compras/movimentações por dia para uso no gráfico de barras do dashboard.
 */
public record VendaDiariaDTO(LocalDate data, BigDecimal total, long quantidade) {}
