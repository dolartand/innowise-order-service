package com.innowise.orderservice.client;

import com.innowise.orderservice.client.dto.UserInfoDto;
import com.innowise.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${user.service.url}",
        configuration = FeignConfig.class,
        fallbackFactory = UserServiceClientFallbackFactory.class
)
public interface UserServiceClient {

    @GetMapping("/api/v1/users/{id}")
    UserInfoDto getUserById(@PathVariable("id") Long id);
}
