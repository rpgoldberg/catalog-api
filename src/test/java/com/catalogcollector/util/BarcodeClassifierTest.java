package com.catalogcollector.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BarcodeClassifierTest {

    @ParameterizedTest
    @CsvSource({
            // ISBN-13 (978/979 prefix)
            "9781234567890, ISBN_13",
            "9791234567890, ISBN_13",

            // ISSN (977 prefix)
            "9770123456789, ISSN",

            // JAN (45/49 prefix, 13 digits)
            "4512345678901, JAN",
            "4901234567890, JAN",

            // EAN-13 (other 13-digit)
            "3012345678901, EAN_13",
            "8412345678901, EAN_13",

            // UPC-A (12 digits)
            "012345678901, UPC_A",
            "850527003646, UPC_A",

            // ISBN-10 (9 digits + digit)
            "0123456789, ISBN_10",

            // ISBN-10 with X check digit
            "012345678X, ISBN_10",
            "012345678x, ISBN_10",

            // EAN-8 (8 digits)
            "12345678, EAN_8",
    })
    void shouldClassifyKnownFormats(String barcode, String expectedType) {
        assertThat(BarcodeClassifier.classify(barcode)).isEqualTo(expectedType);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",           // blank
            "12345",         // wrong length
            "123456789012345", // too long
            "ABCDEFGHIJKL",  // non-numeric 12 chars
            "ABCDEFGHIJKLM", // non-numeric 13 chars
            "ABCDEFGH",      // non-numeric 8 chars
            "12345678A",     // 9 chars invalid (not ISBN-10 format)
            "A234567890",    // ISBN-10 with non-digit in first 9
    })
    void shouldReturnUnknownForInvalidBarcodes(String barcode) {
        assertThat(BarcodeClassifier.classify(barcode)).isEqualTo("UNKNOWN");
    }
}
