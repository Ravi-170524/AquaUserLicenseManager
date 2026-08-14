package com.vassarlabs.aulm.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank String username,
        @NotBlank String projectName,
        @NotBlank String email
) {
}
