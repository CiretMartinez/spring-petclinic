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
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Request payload for scheduling a new pet visit.
 *
 * @param date date of the visit (optional; defaults to tomorrow if null, must be in the
 * future when provided)
 * @param description reason or description for the visit (1-255 chars)
 */
@Schema(description = "Request payload to schedule a new pet visit")
public record VisitRequest(
		@Schema(description = "Date of the visit (must be in the future; defaults to tomorrow if omitted)",
				example = "2027-01-15") @Nullable @Future LocalDate date,
		@Schema(description = "Reason or clinical description of the visit",
				example = "rabies shot") @NotBlank @Size(max = 255) String description) {

}

