package com.deefacto.user_service.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deefacto.user_service.common.dto.ApiResponseDto;
import com.deefacto.user_service.domain.dto.UserRegisterDto;
import com.deefacto.user_service.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UserAuthController {
    private final UserService userService;

    @PostMapping("/users/register")
    public ApiResponseDto<Map<String, String>> registerUser(@RequestBody @Valid UserRegisterDto userRegisterDto) {
        Map<String, String> data = new HashMap<>();
        userService.registerUser(userRegisterDto);
        data.put("employeeId", userRegisterDto.getEmployeeId());

        return ApiResponseDto.createOk(data, "회원 등록 성공");
    }
        
}
