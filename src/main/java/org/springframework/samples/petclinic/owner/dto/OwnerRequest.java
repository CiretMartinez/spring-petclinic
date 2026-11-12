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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating an Owner.
 *
 * @param firstName owner's first name (1-30 chars)
 * @param lastName owner's last name (1-30 chars)
 * @param address street address (1-255 chars)
 * @param city city name (1-80 chars)
 * @param telephone 10-digit numeric telephone number
 */
@Schema(description = "Request payload to create or update a pet owner")
public record OwnerRequest(
		@Schema(description = "First name of the owner", example = "George") @NotBlank @Size(max = 30) String firstName,
		@Schema(description = "Last name of the owner", example = "Franklin") @NotBlank @Size(max = 30) String lastName,
		@Schema(description = "Street address of the owner",
				example = "110 W. Liberty St.") @NotBlank @Size(max = 255) String address,
		@Schema(description = "City of the owner", example = "Madison") @NotBlank @Size(max = 80) String city,
		@Schema(description = "10-digit telephone number", example = "6085551023") @NotBlank @Pattern(
				regexp = "\\d{10}", message = "{telephone.invalid}") String telephone) {

}

