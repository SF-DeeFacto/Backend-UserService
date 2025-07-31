package com.deefacto.user_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.deefacto.user_service.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.deefacto.user_service.domain.dto.UserRegisterDto;
import com.deefacto.user_service.domain.Entitiy.User;


@Service
@Slf4j 
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public void registerUser(UserRegisterDto userRegisterDto) {
        // TODO: 사용자 중복 처리
        User user = userRegisterDto.toEntity();
        // TODO: 비밀번호 암호화 처리
        // TODO: 사용자 권한 처리
        // TODO: 사용자 생성일 처리
        // TODO: 사용자 수정일 처리
        // TODO: 사용자 삭제일 처리
        // TODO: 사용자 삭제 여부 처리
        // TODO: 사용자 삭제 여부 처리
        userRepository.save(user);
    }
}
