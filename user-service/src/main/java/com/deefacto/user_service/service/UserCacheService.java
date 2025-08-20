package com.deefacto.user_service.service;

import com.deefacto.user_service.domain.Entitiy.User;
import com.deefacto.user_service.domain.dto.UserCacheDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserCacheService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // 필요한 유저 정보만 담아 redis에 저장
    public void saveOrUpdateUser(User user, long ttlMinutes) {
        try {
            UserCacheDto cacheDto = new UserCacheDto(
                    user.getId(),
                    user.getEmployeeId(),
                    user.getName(),
                    user.getRole(),
                    user.getScope(),
                    user.getShift()
            );

            String key = "user:" + user.getEmployeeId();
            String value = objectMapper.writeValueAsString(cacheDto);
            redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
