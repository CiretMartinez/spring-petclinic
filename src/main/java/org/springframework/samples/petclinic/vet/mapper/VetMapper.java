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
package org.springframework.samples.petclinic.vet.mapper;

import java.util.List;

import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.dto.SpecialtyResponse;
import org.springframework.samples.petclinic.vet.dto.VetResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper component converting between {@link Vet}/{@link Specialty} JPA entities and Java
 * 21 record DTOs.
 */
@Component
public class VetMapper {

	public SpecialtyResponse toSpecialtyResponse(Specialty specialty) {
		return new SpecialtyResponse(specialty.getId(), specialty.getName());
	}

	public VetResponse toVetResponse(Vet vet) {
		List<SpecialtyResponse> specialties = vet.getSpecialties().stream().map(this::toSpecialtyResponse).toList();
		return new VetResponse(vet.getId(), vet.getFirstName(), vet.getLastName(), specialties,
				vet.getNrOfSpecialties());
	}

}

