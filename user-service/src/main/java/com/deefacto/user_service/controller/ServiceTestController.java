package com.deefacto.user_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deefacto.user_service.common.dto.ApiResponseDto;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ServiceTestController {
    @GetMapping("/test")
    public ApiResponseDto<Map<String, String>> test() {
        Map<String, String> data = new HashMap<>();
        data.put("employeeId", "E2025001");
        return ApiResponseDto.createOk(data, "회원 등록 성공");
    }
}
