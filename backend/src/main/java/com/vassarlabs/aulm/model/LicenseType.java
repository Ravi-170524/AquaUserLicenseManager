package com.vassarlabs.aulm.model;

import java.time.LocalDate;

public enum LicenseType {
    TRIAL,
    STANDARD,
    PREMIUM,
    ADMIN,
    /** Expiry is an explicit date range supplied by the admin, not computed here. */
    CUSTOM;

    /** Null means the license never expires. */
    public LocalDate computeExpiryDate(LocalDate from) {
        return switch (this) {
            case TRIAL -> from.plusWeeks(1);
            case STANDARD -> from.plusMonths(1);
            case PREMIUM -> from.plusYears(1);
            case ADMIN -> null;
            case CUSTOM -> throw new IllegalStateException("CUSTOM license requires an explicit start/end date");
        };
    }
}
