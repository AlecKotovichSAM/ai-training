package com.alec.aitraining.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Stable JSON wrapper around a Spring Data {@link Page}.
 *
 * <p>
 * Using this DTO instead of returning {@code Page}/{@code PageImpl} directly
 * guarantees a stable response contract — independent of Spring Data's internal
 * serialisation changes.
 */
@Schema(description = "Paginated response wrapper with a stable JSON structure")
public record PageResponse<T>(
		@Schema(description = "Page content (items on the current page)") List<T> content,
		@Schema(description = "Zero-based page index", example = "0") int page,
		@Schema(description = "Requested page size", example = "50") int size,
		@Schema(description = "Number of elements on the current page", example = "50") int numberOfElements,
		@Schema(description = "Total number of elements across all pages", example = "1234") long totalElements,
		@Schema(description = "Total number of pages", example = "25") int totalPages,
		@Schema(description = "Whether this is the first page", example = "true") boolean first,
		@Schema(description = "Whether this is the last page", example = "false") boolean last,
		@Schema(description = "Whether the content list is empty", example = "false") boolean empty) {
	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getNumberOfElements(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast(),
				page.isEmpty());
	}
}
