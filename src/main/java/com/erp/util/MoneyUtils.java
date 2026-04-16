package com.erp.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtils {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private MoneyUtils() {
    }

    public static String formatCurrency(BigDecimal value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(PT_BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(nvl(value));
    }

    public static String formatInput(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        format.setParseBigDecimal(true);
        return format.format(nvl(value));
    }

    public static BigDecimal parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String normalized = text.trim()
                .replace("R$", "")
                .replace("\u00A0", "")
                .replace(" ", "")
                .replaceAll("[^0-9,.-]", "");

        if (normalized.isEmpty() || "-".equals(normalized)) {
            return BigDecimal.ZERO;
        }

        int lastComma = normalized.lastIndexOf(',');
        int lastDot = normalized.lastIndexOf('.');

        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                normalized = normalized.replace(".", "").replace(",", ".");
            } else {
                normalized = normalized.replace(",", "");
            }
        } else if (lastComma >= 0) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else if (lastDot >= 0) {
            normalized = normalizeDotOnly(normalized, lastDot);
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String normalizeDotOnly(String value, int lastDot) {
        int decimals = value.length() - lastDot - 1;
        int dotCount = value.length() - value.replace(".", "").length();
        if (dotCount > 1 || decimals == 3) {
            return value.replace(".", "");
        }
        return value;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
