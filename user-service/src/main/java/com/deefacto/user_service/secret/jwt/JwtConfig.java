package com.deefacto.user_service.secret.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(value = "jwt", ignoreUnknownFields = true)
@Getter @Setter
public class JwtConfig {
    private Integer expriresIn;
    private String secretKey;
}
