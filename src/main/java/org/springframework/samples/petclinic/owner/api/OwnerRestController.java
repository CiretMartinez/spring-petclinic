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
package org.springframework.samples.petclinic.owner.api;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.dto.PageResponse;
import org.springframework.samples.petclinic.owner.dto.OwnerRequest;
import org.springframework.samples.petclinic.owner.dto.OwnerResponse;
import org.springframework.samples.petclinic.owner.service.OwnerPetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for managing pet owners under {@code /api/v1/owners}.
 */
@RestController
@RequestMapping("/api/v1/owners")
@Tag(name = "Owners", description = "Operations for managing pet clinic owners")
public class OwnerRestController {

	private final OwnerPetService ownerPetService;

	public OwnerRestController(OwnerPetService ownerPetService) {
		this.ownerPetService = ownerPetService;
	}

	@GetMapping
	@Operation(summary = "Search and list owners with pagination",
			description = "Returns a paginated list of owners whose last name starts with the given filter.")
	@ApiResponse(responseCode = "200", description = "Paginated list of owners retrieved successfully")
	@ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
	public ResponseEntity<PageResponse<OwnerResponse>> listOwners(
			@RequestParam(required = false, defaultValue = "") String lastName,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
		return ResponseEntity.ok(this.ownerPetService.findByLastName(lastName, page, size));
	}

	@GetMapping("/{ownerId}")
	@Operation(summary = "Get owner by ID",
			description = "Returns the owner details including their pets and visit history.")
	@ApiResponse(responseCode = "200", description = "Owner found")
	@ApiResponse(responseCode = "404", description = "Owner not found")
	public ResponseEntity<OwnerResponse> getOwner(@PathVariable int ownerId) {
		return ResponseEntity.ok(this.ownerPetService.findOwnerById(ownerId));
	}

	@PostMapping
	@Operation(summary = "Create a new owner", description = "Creates a new pet owner and returns the created resource.")
	@ApiResponse(responseCode = "201", description = "Owner created successfully")
	@ApiResponse(responseCode = "400", description = "Validation error in request payload")
	public ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody OwnerRequest request) {
		OwnerResponse created = this.ownerPetService.createOwner(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{ownerId}")
	@Operation(summary = "Update an existing owner",
			description = "Updates contact and personal details of an existing owner.")
	@ApiResponse(responseCode = "200", description = "Owner updated successfully")
	@ApiResponse(responseCode = "400", description = "Validation error in request payload")
	@ApiResponse(responseCode = "404", description = "Owner not found")
	public ResponseEntity<OwnerResponse> updateOwner(@PathVariable int ownerId,
			@Valid @RequestBody OwnerRequest request) {
		return ResponseEntity.ok(this.ownerPetService.updateOwner(ownerId, request));
	}

}

