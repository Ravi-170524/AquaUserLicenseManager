package com.vassarlabs.aulm.dto;

public record LoginResponse(
        String token,
        UserResponse user
) {
}
