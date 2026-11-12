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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating a Pet belonging to an Owner.
 *
 * @param name pet's name (1-30 chars, unique per owner)
 * @param birthDate pet's birth date (cannot be in the future)
 * @param typeId identifier of the pet type from the catalog
 */
@Schema(description = "Request payload to register or update a pet")
public record PetRequest(
		@Schema(description = "Pet name (unique per owner)", example = "Leo") @NotBlank @Size(
				max = 30) String name,
		@Schema(description = "Pet birth date (must be in the past or present)",
				example = "2020-09-07") @NotNull @PastOrPresent LocalDate birthDate,
		@Schema(description = "Identifier of the pet type", example = "1") @NotNull Integer typeId) {

}

