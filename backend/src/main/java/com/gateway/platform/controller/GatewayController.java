package com.gateway.platform.controller;

import com.gateway.platform.service.GatewayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Catch-all proxy: every consumer request to /gateway/** lands here and is
 * routed through GatewayService's full pipeline (auth -> rate limit -> quota
 * -> forward -> record). Individual backend APIs are never hardcoded — the
 * route is resolved from the Api table at request time.
 */
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
@Tag(name = "Gateway")
public class GatewayController {

    private final GatewayService gatewayService;

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<String> proxy(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody(required = false) String body,
            jakarta.servlet.http.HttpServletRequest request) {

        String route = request.getRequestURI(); // e.g. "/gateway/products" — matches Api.gatewayRoute exactly
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        return gatewayService.handle(apiKey, route, method, body);
    }
}
