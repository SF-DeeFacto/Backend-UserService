package com.deefacto.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정 클래스
 * 
 * API Gateway에서 이미 JWT 토큰 검증을 완료하고 X-Employee-Id, X-Role 헤더를 추가하여 전달하므로,
 * User Service에서는 별도의 JWT 필터 없이 헤더 기반 인증을 신뢰합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 비밀번호 암호화를 위한 BCrypt 인코더 빈 등록
     * 
     * BCrypt는 단방향 해시 함수로, 비밀번호를 안전하게 저장할 수 있습니다.
     * Salt를 자동으로 생성하여 같은 비밀번호라도 다른 해시값을 생성합니다.
     * 
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 설정
     * 
     * API Gateway 아키텍처에 맞춰 다음과 같이 설정:
     * 1. CSRF 비활성화 (REST API이므로 불필요)
     * 2. 경로별 인증 요구사항 설정
     * 3. JWT 필터 제거 (API Gateway에서 처리)
     * 
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain 설정된 필터 체인
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보호 비활성화 (REST API에서는 불필요)
            .csrf(AbstractHttpConfigurer::disable)
            // 요청별 인증 설정
            .authorizeHttpRequests(auth -> auth
                // 공개 접근 가능한 경로들 (인증 불필요)
                .requestMatchers("/auth/**", "/health/**", "/test/**").permitAll()
                // 사용자 관련 경로는 인증 필요 (API Gateway에서 헤더 검증 완료)
                .requestMatchers("/users/**").authenticated()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            );
        
        // API Gateway에서 이미 JWT 토큰 검증을 완료했으므로
        // X-Employee-Id, X-Role 헤더를 신뢰하여 인증 처리
        // 별도의 JWT 필터는 불필요
        
        return http.build();
    }

} 