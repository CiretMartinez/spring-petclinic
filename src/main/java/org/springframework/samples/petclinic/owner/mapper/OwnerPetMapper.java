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
package org.springframework.samples.petclinic.owner.mapper;

import java.util.List;

import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.owner.dto.OwnerRequest;
import org.springframework.samples.petclinic.owner.dto.OwnerResponse;
import org.springframework.samples.petclinic.owner.dto.PetResponse;
import org.springframework.samples.petclinic.owner.dto.PetResponse.VisitSummary;
import org.springframework.samples.petclinic.owner.dto.PetTypeResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper component responsible for converting between Owner/Pet JPA entities and Java 21
 * record DTOs.
 */
@Component
public class OwnerPetMapper {

	public PetTypeResponse toPetTypeResponse(PetType petType) {
		if (petType == null) {
			return null;
		}
		return new PetTypeResponse(petType.getId(), petType.getName());
	}

	public VisitSummary toVisitSummary(Visit visit) {
		return new VisitSummary(visit.getId(), visit.getDate(), visit.getDescription());
	}

	public PetResponse toPetResponse(Pet pet, Integer ownerId) {
		List<VisitSummary> visitSummaries = pet.getVisits().stream().map(this::toVisitSummary).toList();
		return new PetResponse(pet.getId(), pet.getName(), pet.getBirthDate(), toPetTypeResponse(pet.getType()),
				ownerId, visitSummaries);
	}

	public OwnerResponse toOwnerResponse(Owner owner) {
		Integer ownerId = owner.getId();
		List<PetResponse> pets = owner.getPets().stream().map(pet -> toPetResponse(pet, ownerId)).toList();
		return new OwnerResponse(ownerId, owner.getFirstName(), owner.getLastName(), owner.getAddress(),
				owner.getCity(), owner.getTelephone(), pets);
	}

	public Owner toNewOwnerEntity(OwnerRequest request) {
		Owner owner = new Owner();
		updateOwnerEntity(owner, request);
		return owner;
	}

	public void updateOwnerEntity(Owner owner, OwnerRequest request) {
		owner.setFirstName(request.firstName().strip());
		owner.setLastName(request.lastName().strip());
		owner.setAddress(request.address().strip());
		owner.setCity(request.city().strip());
		owner.setTelephone(request.telephone().strip());
	}

}

