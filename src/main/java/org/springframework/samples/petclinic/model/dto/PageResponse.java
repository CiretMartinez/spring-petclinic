/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.model.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Generic immutable Java 21 record representing a paginated REST response.
 *
 * @param <T> the type of elements in the page
 * @param content the list of elements on the current page
 * @param page the current zero-based page index
 * @param size the size of the page
 * @param totalElements the total number of elements across all pages
 * @param totalPages the total number of pages
 * @param first whether this is the first page
 * @param last whether this is the last page
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean first,
		boolean last) {

	public PageResponse {
		content = content != null ? List.copyOf(content) : List.of();
	}

	public static <T> PageResponse<T> from(Page<T> springPage) {
		return new PageResponse<>(springPage.getContent(), springPage.getNumber(), springPage.getSize(),
				springPage.getTotalElements(), springPage.getTotalPages(), springPage.isFirst(), springPage.isLast());
	}

}

