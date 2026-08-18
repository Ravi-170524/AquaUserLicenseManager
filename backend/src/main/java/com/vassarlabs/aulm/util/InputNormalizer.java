package com.vassarlabs.aulm.util;

import java.util.Locale;

public final class InputNormalizer {

    private InputNormalizer() {
    }

    public static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    public static String lowerTrim(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
