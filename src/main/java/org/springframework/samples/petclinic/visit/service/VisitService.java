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
package org.springframework.samples.petclinic.visit.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.owner.service.OwnerPetService;
import org.springframework.samples.petclinic.system.exception.BusinessRuleException;
import org.springframework.samples.petclinic.system.exception.ResourceNotFoundException;
import org.springframework.samples.petclinic.visit.dto.VisitRequest;
import org.springframework.samples.petclinic.visit.dto.VisitResponse;
import org.springframework.samples.petclinic.visit.mapper.VisitMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application and domain service for managing clinical visits of pets.
 */
@Service
@Transactional(readOnly = true)
public class VisitService {

	private final OwnerPetService ownerPetService;

	private final OwnerRepository owners;

	private final VisitMapper mapper;

	public VisitService(OwnerPetService ownerPetService, OwnerRepository owners, VisitMapper mapper) {
		this.ownerPetService = ownerPetService;
		this.owners = owners;
		this.mapper = mapper;
	}

	public List<VisitResponse> findVisitsByPet(int ownerId, int petId) {
		Owner owner = this.ownerPetService.getOwnerEntityById(ownerId);
		Pet pet = getPetOrThrow(owner, petId);
		return pet.getVisits()
			.stream()
			.map(visit -> this.mapper.toVisitResponse(visit, pet.getId(), owner.getId()))
			.toList();
	}

	@Transactional
	public VisitResponse createVisit(int ownerId, int petId, VisitRequest request) {
		Owner owner = this.ownerPetService.getOwnerEntityById(ownerId);
		Pet pet = getPetOrThrow(owner, petId);

		LocalDate visitDate = (request.date() != null) ? request.date() : LocalDate.now().plusDays(1);
		if (!visitDate.isAfter(LocalDate.now())) {
			throw new BusinessRuleException("Visit date must be in the future");
		}

		Visit visit = new Visit();
		visit.setDate(visitDate);
		visit.setDescription(request.description().strip());

		owner.addVisit(petId, visit);
		Owner savedOwner = this.owners.saveAndFlush(owner);
		Pet savedPet = getPetOrThrow(savedOwner, petId);

		Visit persistedVisit = savedPet.getVisits()
			.stream()
			.reduce((first, second) -> second)
			.orElse(visit);

		return this.mapper.toVisitResponse(persistedVisit, savedPet.getId(), savedOwner.getId());
	}

	private Pet getPetOrThrow(Owner owner, int petId) {
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new ResourceNotFoundException(
					"Pet with id " + petId + " not found for owner with id " + owner.getId() + ".");
		}
		return pet;
	}

}

