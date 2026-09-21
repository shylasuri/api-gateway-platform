package com.gateway.platform.controller;

import com.gateway.platform.dto.response.AnalyticsResponse;
import com.gateway.platform.entity.User;
import com.gateway.platform.repository.UserRepository;
import com.gateway.platform.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @GetMapping("/analytics")
    public AnalyticsResponse platformAnalytics(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.platformAnalytics(days);
    }

    @GetMapping("/consumers")
    public List<User> consumers() {
        return userRepository.findAll();
    }
}
