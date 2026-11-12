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
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.owner.dto.PetRequest;
import org.springframework.samples.petclinic.owner.dto.PetResponse;
import org.springframework.samples.petclinic.owner.service.OwnerPetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for managing pets belonging to a specific owner under
 * {@code /api/v1/owners/{ownerId}/pets}.
 */
@RestController
@RequestMapping("/api/v1/owners/{ownerId}/pets")
@Tag(name = "Pets", description = "Operations for managing an owner's pets")
public class PetRestController {

	private final OwnerPetService ownerPetService;

	public PetRestController(OwnerPetService ownerPetService) {
		this.ownerPetService = ownerPetService;
	}

	@GetMapping
	@Operation(summary = "List pets for an owner", description = "Returns all pets belonging to the specified owner.")
	@ApiResponse(responseCode = "200", description = "List of pets retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Owner not found")
	public ResponseEntity<List<PetResponse>> listPets(@PathVariable int ownerId) {
		return ResponseEntity.ok(this.ownerPetService.findPetsByOwnerId(ownerId));
	}

	@GetMapping("/{petId}")
	@Operation(summary = "Get pet by ID for an owner",
			description = "Returns details of a specific pet belonging to the specified owner.")
	@ApiResponse(responseCode = "200", description = "Pet found")
	@ApiResponse(responseCode = "404", description = "Owner or pet not found")
	public ResponseEntity<PetResponse> getPet(@PathVariable int ownerId, @PathVariable int petId) {
		return ResponseEntity.ok(this.ownerPetService.findPetById(ownerId, petId));
	}

	@PostMapping
	@Operation(summary = "Register a new pet for an owner",
			description = "Creates a new pet for the specified owner enforcing unique pet name per owner.")
	@ApiResponse(responseCode = "201", description = "Pet created successfully")
	@ApiResponse(responseCode = "400", description = "Validation error in request payload")
	@ApiResponse(responseCode = "404", description = "Owner or PetType not found")
	@ApiResponse(responseCode = "409", description = "Duplicate pet name for this owner")
	public ResponseEntity<PetResponse> createPet(@PathVariable int ownerId, @Valid @RequestBody PetRequest request) {
		PetResponse created = this.ownerPetService.createPet(ownerId, request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{petId}")
	@Operation(summary = "Update an existing pet",
			description = "Updates name, birth date, or type of an existing pet belonging to the owner.")
	@ApiResponse(responseCode = "200", description = "Pet updated successfully")
	@ApiResponse(responseCode = "400", description = "Validation error in request payload")
	@ApiResponse(responseCode = "404", description = "Owner, Pet, or PetType not found")
	@ApiResponse(responseCode = "409", description = "Duplicate pet name for this owner")
	public ResponseEntity<PetResponse> updatePet(@PathVariable int ownerId, @PathVariable int petId,
			@Valid @RequestBody PetRequest request) {
		return ResponseEntity.ok(this.ownerPetService.updatePet(ownerId, petId, request));
	}

}

