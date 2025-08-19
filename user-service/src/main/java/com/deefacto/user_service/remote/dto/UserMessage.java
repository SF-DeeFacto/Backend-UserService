package com.deefacto.user_service.remote.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

public class UserMessage {
    // 사용자 조회 요청 메시지
    @Getter
    @Setter
    public static class UserRequestMessage {
        private Long notificationId;
        private String zoneId;
        private String shift; // 예: "A", "B"
    }

    // 사용자 조회 응답 메시지
    @Getter
    @Setter
    public static class UserResponseMessage {
        private Long notificationId;
        private List<Long> userIds; // 조회된 userId 리스트
    }
}
