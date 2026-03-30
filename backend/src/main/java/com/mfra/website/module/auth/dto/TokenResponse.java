package com.mfra.website.module.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
}
