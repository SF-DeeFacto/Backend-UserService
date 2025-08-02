package com.deefacto.user_service.secret.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * JWT 설정을 관리하는 설정 클래스
 * 
 * application.yml의 jwt 설정을 바인딩하여 사용합니다.
 * 환경 변수를 통해 설정값을 동적으로 변경할 수 있습니다.
 */
@Component
@ConfigurationProperties(value = "jwt", ignoreUnknownFields = true)
@Getter @Setter
public class JwtConfig {
    
    /**
     * 리프레시 토큰 만료 시간 (초 단위)
     * 환경 변수: JWT_REFRESH_TOKEN_EXPIRES_IN
     * 기본값: 86400초 (24시간)
     */
    private Integer expriresIn;
    
    /**
     * JWT 서명에 사용할 시크릿 키 (Base64 인코딩)
     * 환경 변수: JWT_SECRET_KEY
     */
    private String secretKey;
    
    /**
     * 액세스 토큰 만료 시간 (초 단위)
     * 환경 변수: JWT_ACCESS_TOKEN_EXPIRES_IN
     * 기본값: 900초 (15분)
     */
    private Integer accessTokenExpiresIn;
}
