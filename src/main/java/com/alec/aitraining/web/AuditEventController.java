package com.alec.aitraining.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.alec.aitraining.dto.AuditEventResponse;
import com.alec.aitraining.dto.AuditEventSearchRequest;
import com.alec.aitraining.dto.CreateAuditEventRequest;
import com.alec.aitraining.dto.PageResponse;
import com.alec.aitraining.service.AuditEventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Audit Events", description = "API for recording and querying audit events")
@RestController
@RequestMapping("/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

	private final AuditEventService service;

	@Operation(summary = "Create an audit event", description = "Accepts a single audit event from another service and appends it to the immutable audit log.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Event successfully created", content = @Content(schema = @Schema(implementation = AuditEventResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content)
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AuditEventResponse create(@Valid @RequestBody CreateAuditEventRequest request) {
		return service.append(request);
	}

	@Operation(summary = "Search audit events", description = "Returns a paginated list of audit events filtered by the provided criteria. All filter parameters are optional.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Paginated list of matching audit events", content = @Content(schema = @Schema(implementation = PageResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content)
	})
	@GetMapping
	@Parameters({
			@Parameter(name = "page", in = ParameterIn.QUERY, description = "Page number (0-based)", example = "0"),
			@Parameter(name = "size", in = ParameterIn.QUERY, description = "Page size", example = "50"),
			@Parameter(name = "sort", in = ParameterIn.QUERY, description = "Sorting criteria in format: property,(asc|desc). Repeat parameter for multiple sort fields.", example = "timestamp,desc")
	})
	public PageResponse<AuditEventResponse> search(
			AuditEventSearchRequest searchRequest,
			@Parameter(hidden = true) @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
		return PageResponse.from(service.search(searchRequest, pageable));
	}
}
