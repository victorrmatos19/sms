package com.erp.model.dto.caixa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CaixaResumoDTO(
        boolean aberto,
        Integer sessaoId,
        String caixaNome,
        String operadorNome,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        BigDecimal saldoInicial,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldoAtual,
        BigDecimal saldoFinalInformado,
        BigDecimal diferenca
) {}
