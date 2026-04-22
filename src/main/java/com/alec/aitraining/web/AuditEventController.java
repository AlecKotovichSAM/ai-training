package com.alec.aitraining.web;

import com.alec.aitraining.dto.AuditEventResponse;
import com.alec.aitraining.dto.AuditEventSearchRequest;
import com.alec.aitraining.dto.CreateAuditEventRequest;
import com.alec.aitraining.service.AuditEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventService service;

    /**
     * Accepts a single audit event from another service.
     *
     * <pre>
     * POST /audit-events
     * {
     *   "actor":    "user:42",
     *   "action":   "invoice.updated",
     *   "resource": "invoice:777",
     *   "outcome":  "SUCCESS",
     *   "context":  { "ip": "10.0.0.1" }
     * }
     * </pre>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEventResponse create(@Valid @RequestBody CreateAuditEventRequest request) {
        return service.append(request);
    }

    /**
     * Searches events with optional filters.
     *
     * <pre>
     * GET /audit-events?actor=user:42&from=2025-01-01T00:00:00Z&page=0&size=50
     * </pre>
     */
    @GetMapping
    public Page<AuditEventResponse> search(
            AuditEventSearchRequest searchRequest,
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable
    ) {
        return service.search(searchRequest, pageable);
    }
}
