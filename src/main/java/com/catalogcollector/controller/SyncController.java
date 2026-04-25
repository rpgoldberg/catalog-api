package com.catalogcollector.controller;

import com.catalogcollector.dto.DeltaSyncResponse;
import com.catalogcollector.dto.SyncPushRequest;
import com.catalogcollector.dto.SyncPushResponse;
import com.catalogcollector.dto.SyncStatusResponse;
import com.catalogcollector.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/delta")
    public ResponseEntity<DeltaSyncResponse> getDelta(
            Authentication authentication,
            @RequestParam Instant since,
            @RequestParam(defaultValue = "500") int limit) {
        UUID userId = (UUID) authentication.getPrincipal();
        int effectiveLimit = Math.min(Math.max(limit, 1), 1000);
        return ResponseEntity.ok(syncService.getDelta(userId, since, effectiveLimit));
    }

    @PostMapping("/push")
    public ResponseEntity<SyncPushResponse> push(
            Authentication authentication,
            @Valid @RequestBody SyncPushRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(syncService.push(userId, request.mutations()));
    }

    @GetMapping("/status")
    public ResponseEntity<SyncStatusResponse> getStatus(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(syncService.getStatus(userId));
    }
}
