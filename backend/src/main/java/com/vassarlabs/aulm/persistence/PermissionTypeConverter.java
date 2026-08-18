package com.vassarlabs.aulm.persistence;

import com.vassarlabs.aulm.model.PermissionType;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PermissionTypeConverter extends LowercaseEnumConverter<PermissionType> {
    public PermissionTypeConverter() {
        super(PermissionType.class);
    }
}
