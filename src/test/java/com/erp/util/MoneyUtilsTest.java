package com.erp.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUtilsTest {

    @Test
    void parse_aceita_virgula_decimal_brasileira() {
        assertThat(MoneyUtils.parse("24,90")).isEqualByComparingTo("24.90");
    }

    @Test
    void parse_aceita_ponto_decimal_sem_transformar_em_milhar() {
        assertThat(MoneyUtils.parse("24.90")).isEqualByComparingTo("24.90");
    }

    @Test
    void parse_aceita_milhar_brasileiro_com_virgula_decimal() {
        assertThat(MoneyUtils.parse("R$ 1.234,56")).isEqualByComparingTo("1234.56");
    }

    @Test
    void parse_trata_ponto_com_tres_digitos_como_milhar() {
        assertThat(MoneyUtils.parse("1.234")).isEqualByComparingTo("1234");
    }

    @Test
    void format_input_usa_duas_casas_com_virgula() {
        assertThat(MoneyUtils.formatInput(new BigDecimal("24.9"))).isEqualTo("24,90");
    }

    @Test
    void format_currency_usa_reais_com_duas_casas() {
        assertThat(MoneyUtils.formatCurrency(new BigDecimal("24.9"))).contains("24,90");
    }
}
