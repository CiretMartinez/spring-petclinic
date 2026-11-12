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
package org.springframework.samples.petclinic.visit.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Immutable DTO representing a scheduled or completed clinical visit for a pet.
 *
 * @param id unique identifier of the visit
 * @param date date of the visit
 * @param description description of the visit
 * @param petId identifier of the pet
 * @param ownerId identifier of the pet's owner
 */
@Schema(description = "Clinical visit details")
public record VisitResponse(
		@Schema(description = "Unique identifier of the visit", example = "1") Integer id,
		@Schema(description = "Date of the visit", example = "2027-01-15") LocalDate date,
		@Schema(description = "Description of the visit", example = "rabies shot") String description,
		@Schema(description = "Identifier of the pet", example = "7") Integer petId,
		@Schema(description = "Identifier of the owner", example = "6") Integer ownerId) {

}

