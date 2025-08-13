package com.deefacto.user_service.remote.service;

import com.deefacto.user_service.domain.repository.UserRepository;
import com.deefacto.user_service.remote.dto.UserMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserRequestConsumer {

    private final KafkaTemplate<String, UserMessage.UserResponseMessage> kafkaTemplate;
    private final UserRepository userRepository;

    @KafkaListener(topics = "user.request", groupId = "user-service-group", properties ={
            JsonDeserializer.VALUE_DEFAULT_TYPE
                    // Header에 들어가는 값 (이벤트 메시지 위치)
                    + ":com.deefacto.user_service.remote.dto.UserMessage$UserRequestMessage"
    })
    public void consumeUserRequest(UserMessage.UserRequestMessage request, Acknowledgment ack) {
        // zoneId, shift 기반으로 사용자 조회 (DB 쿼리 또는 캐시)
        List<Long> userIds = queryUsersByZoneAndShift(request.getZoneId(), request.getShift());

        // 조회된 사용자 리스트를 응답 메시지에 담아 보냄
        UserMessage.UserResponseMessage response = new UserMessage.UserResponseMessage();
        response.setNotificationId(request.getNotificationId());
        response.setUserIds(userIds);

        kafkaTemplate.send("user.response", response);
        ack.acknowledge();
    }

    private List<Long> queryUsersByZoneAndShift(String zoneId, String shift) {
        return userRepository.findUserIdsByRoleAndShift(zoneId, shift);
    }
}

