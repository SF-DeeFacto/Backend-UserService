package com.deefacto.user_service.secret.jwt.dto;

import lombok.*;


@NoArgsConstructor
public class TokenDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JwtToken {
        private String token;
        private Integer expiresIn;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessToken {
        private JwtToken access;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class AccessRefreshToken {
        private final JwtToken access;
        private final JwtToken refresh;
    }
}
