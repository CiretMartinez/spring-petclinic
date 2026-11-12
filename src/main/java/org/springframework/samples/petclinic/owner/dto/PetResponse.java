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
package org.springframework.samples.petclinic.owner.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable DTO representing a Pet and its scheduled/recorded visits.
 *
 * @param id unique identifier of the pet
 * @param name pet's name
 * @param birthDate pet's birth date
 * @param type pet type details
 * @param ownerId identifier of the owner
 * @param visits chronological list of visits for this pet
 */
@Schema(description = "Pet details including type and visit history")
public record PetResponse(
		@Schema(description = "Unique identifier of the pet", example = "1") Integer id,
		@Schema(description = "Name of the pet", example = "Leo") String name,
		@Schema(description = "Birth date of the pet", example = "2010-09-07") LocalDate birthDate,
		@Schema(description = "Pet type") PetTypeResponse type,
		@Schema(description = "Owner identifier", example = "1") Integer ownerId,
		@Schema(description = "Visits recorded for this pet") List<VisitSummary> visits) {

	public PetResponse {
		visits = visits != null ? List.copyOf(visits) : List.of();
	}

	/**
	 * Summary representation of a visit embedded within a pet response.
	 *
	 * @param id unique identifier of the visit
	 * @param date date of the visit
	 * @param description reason or description of the visit
	 */
	@Schema(description = "Summary of a pet visit")
	public record VisitSummary(
			@Schema(description = "Unique identifier of the visit", example = "1") Integer id,
			@Schema(description = "Visit date", example = "2013-01-01") LocalDate date,
			@Schema(description = "Visit description", example = "rabies shot") String description) {
	}

}

