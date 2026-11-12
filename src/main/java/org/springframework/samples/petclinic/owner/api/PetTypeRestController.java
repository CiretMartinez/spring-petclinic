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

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.owner.dto.PetTypeResponse;
import org.springframework.samples.petclinic.owner.service.OwnerPetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for querying the catalog of pet types under {@code /api/v1/pet-types}.
 */
@RestController
@RequestMapping("/api/v1/pet-types")
@Tag(name = "Pet Types", description = "Catalog of supported pet types")
public class PetTypeRestController {

	private final OwnerPetService ownerPetService;

	public PetTypeRestController(OwnerPetService ownerPetService) {
		this.ownerPetService = ownerPetService;
	}

	@GetMapping
	@Operation(summary = "List all pet types",
			description = "Returns the full catalog of pet types ordered alphabetically by name.")
	@ApiResponse(responseCode = "200", description = "List of pet types retrieved successfully")
	public ResponseEntity<List<PetTypeResponse>> listPetTypes() {
		return ResponseEntity.ok(this.ownerPetService.findAllPetTypes());
	}

}

