package com.deefacto.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 비밀번호 암호화 설정 클래스
 * 
 * API Gateway에서 JWT 토큰 검증을 처리하므로,
 * User Service에서는 PasswordEncoder만 제공합니다.
 */
@Configuration
public class SecurityConfig {

    /**
     * 비밀번호 암호화를 위한 BCrypt 인코더 빈 등록
     * 
     * BCrypt는 단방향 해시 함수로, 비밀번호를 안전하게 저장할 수 있습니다.
     * Salt를 자동으로 생성하여 같은 비밀번호라도 다른 해시값을 생성합니다.
     * 
     * @return PasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 비밀번호 암호화를 위한 인터페이스
     */
    public interface PasswordEncoder {
        String encode(String rawPassword);
        boolean matches(String rawPassword, String encodedPassword);
    }
    
    /**
     * BCrypt를 사용한 비밀번호 인코더 구현
     */
    public static class BCryptPasswordEncoder implements PasswordEncoder {
        
        @Override
        public String encode(String rawPassword) {
            return org.mindrot.jbcrypt.BCrypt.hashpw(rawPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
        }
        
        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return org.mindrot.jbcrypt.BCrypt.checkpw(rawPassword, encodedPassword);
        }
    }
} 