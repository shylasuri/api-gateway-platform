package com.gateway.platform.controller;

import com.gateway.platform.dto.request.CreatePlanRequest;
import com.gateway.platform.entity.SubscriptionPlan;
import com.gateway.platform.service.PlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Plans")
public class PlanController {

    private final PlanService planService;

    // Visible to any authenticated consumer (pricing page, plan switcher)
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> list() {
        return ResponseEntity.ok(planService.listAll());
    }

    @PostMapping("/admin/plans")
    public ResponseEntity<SubscriptionPlan> create(@Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(request));
    }

    @PutMapping("/admin/plans/{id}")
    public ResponseEntity<SubscriptionPlan> update(@PathVariable String id, @Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.ok(planService.update(id, request));
    }
}
