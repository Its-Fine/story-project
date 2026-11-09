package com.example.story.auth.dto;

import lombok.Data;

@Data
public class AuthResponseDto {

    private String accessToken;

    private String refreshToken;
}
