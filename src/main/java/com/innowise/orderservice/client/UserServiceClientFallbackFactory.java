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
            public UserInfoDto getUserByEmail(String email) {
                log.error("Fallback for getUserByEmail({}): {}", email,  throwable.getMessage());

                return UserInfoDto.builder()
                        .id(0L)
                        .name("Unavailable")
                        .surname("Unavailable")
                        .email(email)
                        .active(true)
                        .build();
            }
        };
    }
}
