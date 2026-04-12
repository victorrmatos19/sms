package com.erp.util;

/**
 * Utilitário estático para validação e formatação de CPF e CNPJ.
 * Implementa os algoritmos de dígitos verificadores sem dependências externas.
 */
public final class CpfCnpjValidator {

    private CpfCnpjValidator() {}

    // ---- Limpeza ----

    /** Remove pontos, traços, barras e espaços do documento. */
    public static String limpar(String documento) {
        if (documento == null) return "";
        return documento.replaceAll("[.\\-/\\s]", "");
    }

    // ---- CPF ----

    /**
     * Valida CPF após limpar a máscara.
     * Rejeita sequências com todos os dígitos iguais (ex: 111.111.111-11).
     */
    public static boolean validarCpf(String cpf) {
        String d = limpar(cpf);
        if (d.length() != 11) return false;
        if (d.chars().distinct().count() == 1) return false; // todos iguais

        int[] digits = toIntArray(d);

        // Primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) soma += digits[i] * (10 - i);
        int resto = soma % 11;
        int dv1 = resto < 2 ? 0 : 11 - resto;
        if (digits[9] != dv1) return false;

        // Segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) soma += digits[i] * (11 - i);
        resto = soma % 11;
        int dv2 = resto < 2 ? 0 : 11 - resto;
        return digits[10] == dv2;
    }

    /**
     * Valida CNPJ após limpar a máscara.
     * Rejeita sequências com todos os dígitos iguais.
     */
    public static boolean validarCnpj(String cnpj) {
        String d = limpar(cnpj);
        if (d.length() != 14) return false;
        if (d.chars().distinct().count() == 1) return false;

        int[] digits = toIntArray(d);
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        // Primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 12; i++) soma += digits[i] * pesos1[i];
        int resto = soma % 11;
        int dv1 = resto < 2 ? 0 : 11 - resto;
        if (digits[12] != dv1) return false;

        // Segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 13; i++) soma += digits[i] * pesos2[i];
        resto = soma % 11;
        int dv2 = resto < 2 ? 0 : 11 - resto;
        return digits[13] == dv2;
    }

    // ---- Formatação ----

    /** Formata CPF limpo para 000.000.000-00. */
    public static String formatarCpf(String cpf) {
        String d = limpar(cpf);
        if (d.length() != 11) return cpf;
        return d.substring(0, 3) + "." + d.substring(3, 6) + "."
             + d.substring(6, 9) + "-" + d.substring(9, 11);
    }

    /** Formata CNPJ limpo para 00.000.000/0000-00. */
    public static String formatarCnpj(String cnpj) {
        String d = limpar(cnpj);
        if (d.length() != 14) return cnpj;
        return d.substring(0, 2) + "." + d.substring(2, 5) + "."
             + d.substring(5, 8) + "/" + d.substring(8, 12) + "-" + d.substring(12, 14);
    }

    // ---- Helper ----

    private static int[] toIntArray(String s) {
        int[] arr = new int[s.length()];
        for (int i = 0; i < s.length(); i++) arr[i] = s.charAt(i) - '0';
        return arr;
    }
}
