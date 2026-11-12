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
package org.springframework.samples.petclinic.vet.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable DTO representing a veterinarian and their specialties.
 *
 * @param id unique identifier of the veterinarian
 * @param firstName veterinarian's first name
 * @param lastName veterinarian's last name
 * @param specialties sorted list of specialties
 * @param nrOfSpecialties total count of specialties
 */
@Schema(description = "Veterinarian details including specialties")
public record VetResponse(
		@Schema(description = "Unique identifier of the veterinarian", example = "1") Integer id,
		@Schema(description = "First name", example = "James") String firstName,
		@Schema(description = "Last name", example = "Carter") String lastName,
		@Schema(description = "List of specialties ordered alphabetically") List<SpecialtyResponse> specialties,
		@Schema(description = "Number of specialties", example = "0") int nrOfSpecialties) {

	public VetResponse {
		specialties = specialties != null ? List.copyOf(specialties) : List.of();
	}

}

