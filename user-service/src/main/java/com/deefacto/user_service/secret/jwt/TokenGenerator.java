package com.deefacto.user_service.secret.jwt;

import javax.crypto.SecretKey;
import java.util.Date;

import com.deefacto.user_service.domain.Entitiy.User;
import com.deefacto.user_service.domain.repository.UserRepository;
import com.deefacto.user_service.remote.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.deefacto.user_service.secret.jwt.dto.TokenDto;
import com.deefacto.user_service.common.exception.BadParameter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 토큰 생성 및 검증을 담당하는 컴포넌트
 * 
 * 주요 기능:
 * 1. JWT 토큰 생성 (액세스 토큰, 리프레시 토큰)
 * 2. JWT 토큰 검증 및 파싱
 * 3. 토큰에서 사용자 정보 추출
 * 4. 토큰 만료 시간 관리
 * 
 * API Gateway 아키텍처에서:
 * - 토큰 생성: User Service에서 담당
 * - 토큰 검증: API Gateway에서 담당
 * - 이 클래스의 검증 메서드들은 로그아웃 처리용으로만 사용
 */
@Component
@RequiredArgsConstructor
public class TokenGenerator {

    // JWT 설정 정보 (시크릿 키, 만료 시간 등)
    private final JwtConfig jwtConfig;
    
    // JWT 서명에 사용할 시크릿 키 (지연 초기화)
    private volatile SecretKey secretKey;


    private final UserRepository userRepository;

    private final UserCacheService userCacheService;

    /**
     * JWT 서명에 사용할 시크릿 키를 지연 초기화로 생성
     * 
     * Thread-safe한 방식으로 시크릿 키를 한 번만 생성하여 재사용
     * 
     * @return JWT 서명용 SecretKey
     */
    private SecretKey getSecretKey() {
        if (secretKey == null) {
            synchronized (this) {
                if (secretKey == null) {
                    // Base64로 인코딩된 시크릿 키를 디코딩하여 SecretKey 생성
                    secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getSecretKey()));
                }
            }
        }
        return secretKey;
    }

    /**
     * 토큰 타입에 따른 만료 시간을 반환
     * 
     * @param isRefreshToken 리프레시 토큰 여부
     * @return 토큰 만료 시간 (초 단위)
     */
    private int getExpriresIn(boolean isRefreshToken) {
        int expriresIn = 0;

        if (isRefreshToken) {
            // 리프레시 토큰: 설정 파일에서 가져온 만료 시간 사용 (환경 변수에서 읽어옴)
            expriresIn = jwtConfig.getExpriresIn();
        } else {
            // 액세스 토큰: 설정 파일에서 가져온 만료 시간 사용 (환경 변수에서 읽어옴)
            expriresIn = jwtConfig.getAccessTokenExpiresIn();
        }

        return expriresIn;
    }
    
    /**
     * JWT 토큰을 생성하는 메서드
     * 
     * @param employeeId 사용자 사원번호
     * @param isRefreshToken 리프레시 토큰 여부
     * @return 생성된 JWT 토큰과 만료 시간 정보
     */
    // Refresh Token의 경우, userId만 있어도 되지만, 같은 메소드 사용으로 일단 동일한 데이터 저장
    public TokenDto.JwtToken generateJwtToken(String employeeId, boolean isRefreshToken) {
        // 토큰 타입에 따른 만료 시간 계산
        int expriresIn = getExpriresIn(isRefreshToken);
        String tokenType = isRefreshToken ? "refresh" : "access";

        // employeeId 기반 User 정보 조회 (DB)
        User user = userRepository.findByEmployeeId(employeeId);
        
        // JWT 토큰 생성 (userId, shift, role 정보 추가)
        String token = Jwts.builder()
            .issuer("deefacto")                    // 토큰 발급자
            .setSubject(employeeId)                // 토큰 주체 (사용자 ID)
            .claim("EmployeeId", employeeId)       // 사용자 사원번호 클레임
//                .claim("UserId", user.getId())
//                .claim("Shift", user.getShift())
//                .claim("Role", user.getRole())
            .claim("type", tokenType)              // 토큰 타입 클레임 (access/refresh)
            .issuedAt(new Date())                  // 토큰 발급 시간
            .expiration(new Date(System.currentTimeMillis() + expriresIn * 1000L))  // 토큰 만료 시간
            .signWith(getSecretKey())              // 시크릿 키로 서명
            .header().add("typ", "JWT")            // JWT 타입 헤더 추가
            .and()
            .compact();                            // 최종 토큰 문자열 생성

        return new TokenDto.JwtToken(token, expriresIn);
    }

    /**
     * 액세스 토큰만 생성하는 메서드
     * 
     * @param employeeId 사용자 사원번호
     * @return 액세스 토큰 정보
     */
    public TokenDto.AccessToken generateAccessToken(String employeeId) {
        TokenDto.JwtToken jwtToken = this.generateJwtToken(employeeId, false);
        TokenDto.AccessToken accessToken = new TokenDto.AccessToken();
        accessToken.setAccess(jwtToken);
        return accessToken;
    }

    /**
     * 액세스 토큰과 리프레시 토큰을 모두 생성하는 메서드
     * 
     * @param employeeId 사용자 사원번호
     * @return 액세스 토큰과 리프레시 토큰 정보
     */
    public TokenDto.AccessRefreshToken generateAccessRefreshToken(String employeeId) {
        TokenDto.JwtToken accessToken = this.generateJwtToken(employeeId, false);
        TokenDto.JwtToken refreshToken = this.generateJwtToken(employeeId, true);
        return new TokenDto.AccessRefreshToken(accessToken, refreshToken);
    }

    /**
     * JWT 토큰의 유효성을 검증하는 메서드
     * 
     * API Gateway에서 이미 처리하므로 User Service에서는 로그아웃 처리용으로만 사용
     * 
     * @param token 검증할 JWT 토큰
     * @return 토큰 유효성 여부
     */
    public boolean validateToken(String token) {
        try {
            // JWT 파서를 사용하여 토큰 서명 검증
            Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // 토큰 파싱 실패 시 유효하지 않은 토큰으로 판단
            return false;
        }
    }
    
    /**
     * JWT 토큰에서 사용자 사원번호를 추출하는 메서드
     * 
     * API Gateway에서 이미 처리하므로 User Service에서는 로그아웃 처리용으로만 사용
     * 
     * @param token JWT 토큰
     * @return 사용자 사원번호
     */
    public String getEmployeeIdFromToken(String token) {
        // JWT 토큰을 파싱하여 클레임 정보 추출
        Claims claims = Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.getSubject();  // 토큰의 주체(subject) 반환
    }
    
    /**
     * JWT 토큰의 만료 여부를 확인하는 메서드
     * 
     * API Gateway에서 이미 처리하므로 User Service에서는 로그아웃 처리용으로만 사용
     * 
     * @param token JWT 토큰
     * @return 토큰 만료 여부 (true: 만료됨, false: 유효함)
     */
    public boolean isTokenExpired(String token) {
        // JWT 토큰을 파싱하여 클레임 정보 추출
        Claims claims = Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        // 현재 시간과 토큰 만료 시간을 비교
        return claims.getExpiration().before(new Date());
    }
    
    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급하는 메서드
     * 
     * @param refreshToken 리프레시 토큰
     * @return 새로운 액세스 토큰
     * @throws BadParameter 리프레시 토큰이 유효하지 않거나 만료된 경우
     */
    public TokenDto.AccessToken refreshAccessToken(String refreshToken) {
        // 리프레시 토큰 유효성 검증
        if (!validateToken(refreshToken)) {
            throw new BadParameter("Invalid refresh token");
        }
        
        // 리프레시 토큰 만료 확인
        if (isTokenExpired(refreshToken)) {
            throw new BadParameter("Expired refresh token");
        }
        
        // 리프레시 토큰에서 사용자 ID 추출
        String employeeId = getEmployeeIdFromToken(refreshToken);

        // 리프레시 토큰 연장 시 user 정보도 연장
        userCacheService.refreshUserCacheTTL(employeeId,60);

        // 새로운 액세스 토큰 생성
        return generateAccessToken(employeeId);
    }
    
    /**
     * JWT 토큰에서 토큰 타입을 추출하는 메서드
     * 
     * @param token JWT 토큰
     * @return 토큰 타입 ("access" 또는 "refresh")
     */
    public String getTokenType(String token) {
        // JWT 토큰을 파싱하여 클레임 정보 추출
        Claims claims = Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.get("type", String.class);  // 토큰 타입 클레임 반환
    }
    
    /**
     * JWT 토큰의 남은 만료 시간을 계산하는 메서드
     * 
     * @param token JWT 토큰
     * @return 토큰 만료까지 남은 시간 (밀리초 단위)
     */
    public long getExpirationTime(String token) {
        // JWT 토큰을 파싱하여 클레임 정보 추출
        Claims claims = Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        // 토큰 만료 시간에서 현재 시간을 빼서 남은 시간 계산
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
}
