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

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable DTO representing an Owner and their registered pets.
 *
 * @param id unique identifier of the owner
 * @param firstName owner's first name
 * @param lastName owner's last name
 * @param address owner's street address
 * @param city owner's city
 * @param telephone owner's 10-digit telephone number
 * @param pets list of pets belonging to this owner
 */
@Schema(description = "Owner details including owned pets and their visits")
public record OwnerResponse(
		@Schema(description = "Unique identifier of the owner", example = "1") Integer id,
		@Schema(description = "First name", example = "George") String firstName,
		@Schema(description = "Last name", example = "Franklin") String lastName,
		@Schema(description = "Street address", example = "110 W. Liberty St.") String address,
		@Schema(description = "City", example = "Madison") String city,
		@Schema(description = "10-digit telephone number", example = "6085551023") String telephone,
		@Schema(description = "Pets owned by this person") List<PetResponse> pets) {

	public OwnerResponse {
		pets = pets != null ? List.copyOf(pets) : List.of();
	}

}

