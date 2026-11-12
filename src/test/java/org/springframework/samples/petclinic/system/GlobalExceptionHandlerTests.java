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
package org.springframework.samples.petclinic.system;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.samples.petclinic.system.exception.BusinessRuleException;
import org.springframework.samples.petclinic.system.exception.DuplicateResourceException;
import org.springframework.samples.petclinic.system.exception.GlobalExceptionHandler;
import org.springframework.samples.petclinic.system.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler} verifying RFC 7807 {@link ProblemDetail}
 * contracts.
 */
class GlobalExceptionHandlerTests {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void shouldMapResourceNotFoundTo404() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/owners/999");
		ProblemDetail detail = this.handler.handleResourceNotFound(new ResourceNotFoundException("Owner", 999),
				request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(detail.getTitle()).isEqualTo("Resource Not Found");
		assertThat(detail.getDetail()).contains("Owner not found with id: 999");
	}

	@Test
	void shouldMapDuplicateResourceTo409() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/owners/1/pets");
		ProblemDetail detail = this.handler
			.handleDuplicateResource(new DuplicateResourceException("Duplicate pet name"), request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(detail.getTitle()).isEqualTo("Duplicate Resource Conflict");
	}

	@Test
	void shouldMapDataIntegrityViolationTo409() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/owners/1/pets");
		ProblemDetail detail = this.handler
			.handleDataIntegrityViolation(new DataIntegrityViolationException("unique_owner_pet_name"), request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(detail.getTitle()).isEqualTo("Data Integrity Violation");
	}

	@Test
	void shouldMapBusinessRuleExceptionTo400() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/owners/1/pets/1/visits");
		ProblemDetail detail = this.handler
			.handleBusinessRule(new BusinessRuleException("Visit date must be in the future"), request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(detail.getTitle()).isEqualTo("Business Rule Violation");
	}

	@Test
	void shouldMapUnexpectedExceptionTo500() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/owners");
		ProblemDetail detail = this.handler.handleUnexpectedException(new RuntimeException("Simulated failure"),
				request);

		assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(detail.getTitle()).isEqualTo("Internal Server Error");
	}

}

