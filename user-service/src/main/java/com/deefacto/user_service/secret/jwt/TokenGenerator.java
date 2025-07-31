package com.deefacto.user_service.secret.jwt;

import javax.crypto.SecretKey;
import java.util.Date;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.deefacto.user_service.secret.jwt.dto.TokenDto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
@RequiredArgsConstructor
public class TokenGenerator {

    private final JwtConfig jwtConfig;
    private volatile SecretKey secretKey;

    private SecretKey getSecretKey() {
        if (secretKey == null) {
            synchronized (this) {
                if (secretKey == null) {}
                secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getSecretKey()));
            }
        }
        return secretKey;
    }

    private int getExpriresIn(boolean isRefreshToken) {
        int expriresIn = 0;

        if (isRefreshToken) {
            expriresIn = jwtConfig.getExpriresIn();
        } else {
            expriresIn = 60 * 15; // 15분
        }

        return expriresIn;
    }
    public TokenDto.JwtToken generateJwtToken(String employeeId, boolean isRefreshToken) {
        int expriresIn = getExpriresIn(isRefreshToken);
        String tokenType = isRefreshToken ? "refresh" : "access";
        String token = Jwts.builder()
            .issuer("deefacto")
            .setSubject(employeeId)
            .claim("EmployeeId", employeeId)
            .claim("type", tokenType)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expriresIn * 1000L))
            .signWith(getSecretKey())
            .header().add("typ", "JWT")
            .and()
            .compact();

        return new TokenDto.JwtToken(token, expriresIn);
    }

    public TokenDto.AccessToken generateAccessToken(String employeeId) {
        TokenDto.JwtToken jwtToken = this.generateJwtToken(employeeId, false);
        TokenDto.AccessToken accessToken = new TokenDto.AccessToken();
        accessToken.setAccess(jwtToken);
        return accessToken;
    }

    public TokenDto.AccessRefreshToken generateAccessRefreshToken(String employeeId) {
        TokenDto.JwtToken accessToken = this.generateJwtToken(employeeId, false);
        TokenDto.JwtToken refreshToken = this.generateJwtToken(employeeId, true);
        return new TokenDto.AccessRefreshToken(accessToken, refreshToken);
    }

    // TODO: JWT 토큰 검증 로직 구현
    // - validateToken(String token): 토큰 유효성 검증
    // - getEmployeeIdFromToken(String token): 토큰에서 사용자 ID 추출
    // - isTokenExpired(String token): 토큰 만료 확인
    // - refreshToken(String refreshToken): 리프레시 토큰으로 새 액세스 토큰 발급
}
