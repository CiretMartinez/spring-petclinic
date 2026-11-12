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
package org.springframework.samples.petclinic.visit.api;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.visit.dto.VisitRequest;
import org.springframework.samples.petclinic.visit.dto.VisitResponse;
import org.springframework.samples.petclinic.visit.service.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for scheduling and querying clinical visits for a pet under
 * {@code /api/v1/owners/{ownerId}/pets/{petId}/visits}.
 */
@RestController
@RequestMapping("/api/v1/owners/{ownerId}/pets/{petId}/visits")
@Tag(name = "Visits", description = "Operations for scheduling and viewing pet visits")
public class VisitRestController {

	private final VisitService visitService;

	public VisitRestController(VisitService visitService) {
		this.visitService = visitService;
	}

	@GetMapping
	@Operation(summary = "List visits for a pet",
			description = "Returns all visits recorded for the specified pet ordered chronologically.")
	@ApiResponse(responseCode = "200", description = "List of visits retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Owner or pet not found")
	public ResponseEntity<List<VisitResponse>> listVisits(@PathVariable int ownerId, @PathVariable int petId) {
		return ResponseEntity.ok(this.visitService.findVisitsByPet(ownerId, petId));
	}

	@PostMapping
	@Operation(summary = "Schedule a new visit for a pet",
			description = "Books a new future visit for the specified pet.")
	@ApiResponse(responseCode = "201", description = "Visit booked successfully")
	@ApiResponse(responseCode = "400", description = "Validation error or non-future visit date")
	@ApiResponse(responseCode = "404", description = "Owner or pet not found")
	public ResponseEntity<VisitResponse> createVisit(@PathVariable int ownerId, @PathVariable int petId,
			@Valid @RequestBody VisitRequest request) {
		VisitResponse created = this.visitService.createVisit(ownerId, petId, request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

}

