package com.innowise.orderservice.client;

import com.innowise.orderservice.client.dto.UserInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Circuit breaker fallback for UserServiceClient
 */
@Component
@Slf4j
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public  UserServiceClient create(Throwable throwable) {
        return new UserServiceClient() {
            @Override
            public UserInfoDto getUserById(Long id) {
                log.error("Fallback for getUserById({}): {}", id,  throwable.getMessage());

                return UserInfoDto.builder()
                        .id(id)
                        .name("Unavailable")
                        .surname("Unavailable")
                        .email("Unavailable@unavailable.com")
                        .active(true)
                        .build();
            }
        };
    }
}