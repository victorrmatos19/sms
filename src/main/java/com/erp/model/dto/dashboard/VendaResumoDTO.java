package com.erp.model.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VendaResumoDTO(
        String numero,
        String cliente,
        String vendedor,
        LocalDateTime dataVenda,
        BigDecimal valorTotal,
        String formaPagamento
) {}
