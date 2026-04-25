package com.catalogcollector.util;

public final class BarcodeClassifier {

    private BarcodeClassifier() {
    }

    public static String classify(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return "UNKNOWN";
        }

        String trimmed = barcode.trim();

        return switch (trimmed.length()) {
            case 13 -> classify13(trimmed);
            case 12 -> isAllDigits(trimmed) ? "UPC_A" : "UNKNOWN";
            case 10 -> isIsbn10(trimmed) ? "ISBN_10" : "UNKNOWN";
            case 8 -> isAllDigits(trimmed) ? "EAN_8" : "UNKNOWN";
            default -> "UNKNOWN";
        };
    }

    private static String classify13(String barcode) {
        if (!isAllDigits(barcode)) {
            return "UNKNOWN";
        }
        if (barcode.startsWith("978") || barcode.startsWith("979")) {
            return "ISBN_13";
        }
        if (barcode.startsWith("977")) {
            return "ISSN";
        }
        if (barcode.startsWith("45") || barcode.startsWith("49")) {
            return "JAN";
        }
        return "EAN_13";
    }

    private static boolean isIsbn10(String barcode) {
        // First 9 characters must be digits, last can be digit or 'X'
        for (int i = 0; i < 9; i++) {
            if (!Character.isDigit(barcode.charAt(i))) {
                return false;
            }
        }
        char last = barcode.charAt(9);
        return Character.isDigit(last) || last == 'X' || last == 'x';
    }

    private static boolean isAllDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
