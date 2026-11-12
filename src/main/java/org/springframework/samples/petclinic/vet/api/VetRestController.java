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
package org.springframework.samples.petclinic.vet.api;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.dto.PageResponse;
import org.springframework.samples.petclinic.vet.dto.VetResponse;
import org.springframework.samples.petclinic.vet.service.VetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for querying veterinarians and their specialties under
 * {@code /api/v1/vets}.
 */
@RestController
@RequestMapping("/api/v1/vets")
@Tag(name = "Veterinarians", description = "Operations for querying veterinarians and their specialties")
public class VetRestController {

	private final VetService vetService;

	public VetRestController(VetService vetService) {
		this.vetService = vetService;
	}

	@GetMapping
	@Operation(summary = "List veterinarians with pagination",
			description = "Returns a paginated list of veterinarians and their specialties (cached).")
	@ApiResponse(responseCode = "200", description = "Paginated list of veterinarians retrieved successfully")
	@ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
	public ResponseEntity<PageResponse<VetResponse>> listVets(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "5") int size) {
		return ResponseEntity.ok(this.vetService.findPaginated(page, size));
	}

	@GetMapping("/all")
	@Operation(summary = "List all veterinarians without pagination",
			description = "Returns the complete list of veterinarians and their specialties (cached).")
	@ApiResponse(responseCode = "200", description = "Complete list of veterinarians retrieved successfully")
	public ResponseEntity<List<VetResponse>> listAllVets() {
		return ResponseEntity.ok(this.vetService.findAll());
	}

}

