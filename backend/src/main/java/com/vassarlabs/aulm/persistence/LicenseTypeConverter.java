package com.vassarlabs.aulm.persistence;

import com.vassarlabs.aulm.model.LicenseType;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LicenseTypeConverter extends LowercaseEnumConverter<LicenseType> {
    public LicenseTypeConverter() {
        super(LicenseType.class);
    }
}
