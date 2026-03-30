package com.mfra.website.module.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserInfoResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private String avatarUrl;
}
