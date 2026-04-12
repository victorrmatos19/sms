package com.erp.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do CpfCnpjValidator.
 * Cobre CF-61 a CF-67.
 */
class CpfCnpjValidatorTest {

    // ---- CF-61: CPFs válidos ----

    @Test
    void dado_cpf_valido_111444777_35_quando_validar_entao_retorna_true() {
        assertThat(CpfCnpjValidator.validarCpf("111.444.777-35")).isTrue();
    }

    @Test
    void dado_cpf_valido_12345678909_quando_validar_entao_retorna_true() {
        assertThat(CpfCnpjValidator.validarCpf("123.456.789-09")).isTrue();
    }

    @Test
    void dado_cpf_valido_sem_mascara_quando_validar_entao_retorna_true() {
        assertThat(CpfCnpjValidator.validarCpf("12345678909")).isTrue();
    }

    @Test
    void dado_cpf_valido_11144477735_quando_validar_entao_retorna_true() {
        // 111.444.777-35 — dígitos verificadores calculados: resto=8→dv1=3, resto=6→dv2=5
        assertThat(CpfCnpjValidator.validarCpf("111.444.777-35")).isTrue();
    }

    @Test
    void dado_cpf_valido_52998224725_quando_validar_entao_retorna_true() {
        assertThat(CpfCnpjValidator.validarCpf("529.982.247-25")).isTrue();
    }

    // ---- CF-62: CPFs inválidos ----

    @Test
    void dado_cpf_com_digitos_verificadores_errados_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCpf("123.456.789-00")).isFalse();
    }

    @Test
    void dado_cpf_com_todos_zeros_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCpf("000.000.000-00")).isFalse();
    }

    @Test
    void dado_cpf_com_sequencia_repetida_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCpf("111.111.111-11")).isFalse();
    }

    @Test
    void dado_cpf_vazio_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCpf("")).isFalse();
    }

    @Test
    void dado_cpf_nulo_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCpf(null)).isFalse();
    }

    @Test
    void dado_cpf_incompleto_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCpf("123.456")).isFalse();
    }

    // ---- CF-63: CNPJs válidos ----

    @Test
    void dado_cnpj_valido_11222333000181_quando_validar_entao_retorna_true() {
        assertThat(CpfCnpjValidator.validarCnpj("11.222.333/0001-81")).isTrue();
    }

    @Test
    void dado_cnpj_valido_google_brasil_quando_validar_entao_retorna_true() {
        assertThat(CpfCnpjValidator.validarCnpj("34.028.316/0001-03")).isTrue();
    }

    @Test
    void dado_cnpj_valido_bradesco_quando_validar_entao_retorna_true() {
        assertThat(CpfCnpjValidator.validarCnpj("60.701.190/0001-04")).isTrue();
    }

    @Test
    void dado_cnpj_valido_sem_mascara_quando_validar_entao_retorna_true() {
        assertThat(CpfCnpjValidator.validarCnpj("11222333000181")).isTrue();
    }

    // ---- CF-64: CNPJs inválidos ----

    @Test
    void dado_cnpj_com_sequencia_repetida_11_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCnpj("11.111.111/1111-11")).isFalse();
    }

    @Test
    void dado_cnpj_com_todos_zeros_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCnpj("00.000.000/0000-00")).isFalse();
    }

    @Test
    void dado_cnpj_com_digitos_verificadores_errados_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCnpj("11.222.333/0001-00")).isFalse();
    }

    @Test
    void dado_cnpj_vazio_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCnpj("")).isFalse();
    }

    @Test
    void dado_cnpj_nulo_quando_validar_entao_retorna_false() {
        assertThat(CpfCnpjValidator.validarCnpj(null)).isFalse();
    }

    // ---- CF-65: Formatação CPF ----

    @Test
    void dado_cpf_limpo_quando_formatar_entao_retorna_com_mascara() {
        assertThat(CpfCnpjValidator.formatarCpf("12345678909"))
            .isEqualTo("123.456.789-09");
    }

    @Test
    void dado_cpf_ja_com_mascara_quando_formatar_entao_idempotente() {
        assertThat(CpfCnpjValidator.formatarCpf("123.456.789-09"))
            .isEqualTo("123.456.789-09");
    }

    // ---- CF-66: Formatação CNPJ ----

    @Test
    void dado_cnpj_limpo_quando_formatar_entao_retorna_com_mascara() {
        assertThat(CpfCnpjValidator.formatarCnpj("11222333000181"))
            .isEqualTo("11.222.333/0001-81");
    }

    @Test
    void dado_cnpj_ja_com_mascara_quando_formatar_entao_idempotente() {
        assertThat(CpfCnpjValidator.formatarCnpj("11.222.333/0001-81"))
            .isEqualTo("11.222.333/0001-81");
    }

    // ---- CF-67: Limpeza de documento ----

    @Test
    void dado_cpf_com_mascara_quando_limpar_entao_retorna_apenas_digitos() {
        assertThat(CpfCnpjValidator.limpar("123.456.789-09"))
            .isEqualTo("12345678909");
    }

    @Test
    void dado_cnpj_com_mascara_quando_limpar_entao_retorna_apenas_digitos() {
        assertThat(CpfCnpjValidator.limpar("11.222.333/0001-81"))
            .isEqualTo("11222333000181");
    }

    @Test
    void dado_documento_nulo_quando_limpar_entao_retorna_string_vazia() {
        assertThat(CpfCnpjValidator.limpar(null)).isEqualTo("");
    }
}
