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
package org.springframework.samples.petclinic.owner.service;

import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.dto.PageResponse;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.owner.dto.OwnerRequest;
import org.springframework.samples.petclinic.owner.dto.OwnerResponse;
import org.springframework.samples.petclinic.owner.dto.PetRequest;
import org.springframework.samples.petclinic.owner.dto.PetResponse;
import org.springframework.samples.petclinic.owner.dto.PetTypeResponse;
import org.springframework.samples.petclinic.owner.mapper.OwnerPetMapper;
import org.springframework.samples.petclinic.system.exception.DuplicateResourceException;
import org.springframework.samples.petclinic.system.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application and domain service for managing Owners, Pets, and PetTypes.
 */
@Service
@Transactional(readOnly = true)
public class OwnerPetService {

	private final OwnerRepository owners;

	private final PetTypeRepository petTypes;

	private final OwnerPetMapper mapper;

	public OwnerPetService(OwnerRepository owners, PetTypeRepository petTypes, OwnerPetMapper mapper) {
		this.owners = owners;
		this.petTypes = petTypes;
		this.mapper = mapper;
	}

	public PageResponse<OwnerResponse> findByLastName(String lastName, int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page index must not be less than zero");
		}
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("Page size must be between 1 and 100");
		}
		String normalizedLastName = (lastName == null) ? "" : lastName.strip();
		Pageable pageable = PageRequest.of(page, size);
		Page<OwnerResponse> ownerPage = this.owners.findByLastNameStartingWith(normalizedLastName, pageable)
			.map(this.mapper::toOwnerResponse);
		return PageResponse.from(ownerPage);
	}

	public OwnerResponse findOwnerById(int ownerId) {
		Owner owner = getOwnerEntityById(ownerId);
		return this.mapper.toOwnerResponse(owner);
	}

	public Owner getOwnerEntityById(int ownerId) {
		return this.owners.findById(ownerId).orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));
	}

	@Transactional
	public OwnerResponse createOwner(OwnerRequest request) {
		Owner owner = this.mapper.toNewOwnerEntity(request);
		Owner saved = this.owners.save(owner);
		return this.mapper.toOwnerResponse(saved);
	}

	@Transactional
	public OwnerResponse updateOwner(int ownerId, OwnerRequest request) {
		Owner owner = getOwnerEntityById(ownerId);
		this.mapper.updateOwnerEntity(owner, request);
		Owner saved = this.owners.save(owner);
		return this.mapper.toOwnerResponse(saved);
	}

	public List<PetTypeResponse> findAllPetTypes() {
		return this.petTypes.findPetTypes().stream().map(this.mapper::toPetTypeResponse).toList();
	}

	public List<PetResponse> findPetsByOwnerId(int ownerId) {
		Owner owner = getOwnerEntityById(ownerId);
		return owner.getPets().stream().map(pet -> this.mapper.toPetResponse(pet, owner.getId())).toList();
	}

	public PetResponse findPetById(int ownerId, int petId) {
		Owner owner = getOwnerEntityById(ownerId);
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new ResourceNotFoundException(
					"Pet with id " + petId + " not found for owner with id " + ownerId + ".");
		}
		return this.mapper.toPetResponse(pet, owner.getId());
	}

	@Transactional
	public PetResponse createPet(int ownerId, PetRequest request) {
		Owner owner = getOwnerEntityById(ownerId);
		String normalizedName = request.name().strip();

		if (owner.getPet(normalizedName, true) != null) {
			throw new DuplicateResourceException(
					"Pet with name '" + normalizedName + "' already exists for owner with id " + ownerId);
		}

		PetType petType = findPetTypeOrThrow(request.typeId());

		Pet pet = new Pet();
		pet.setName(normalizedName);
		pet.setBirthDate(request.birthDate());
		pet.setType(petType);

		try {
			owner.addPet(pet);
			this.owners.saveAndFlush(owner);
		}
		catch (DataIntegrityViolationException ex) {
			if (isDuplicatePetNameViolation(ex)) {
				throw new DuplicateResourceException(
						"Pet with name '" + normalizedName + "' already exists for owner with id " + ownerId, ex);
			}
			throw ex;
		}

		Pet savedPet = owner.getPet(normalizedName, false);
		return this.mapper.toPetResponse(savedPet != null ? savedPet : pet, owner.getId());
	}

	@Transactional
	public PetResponse updatePet(int ownerId, int petId, PetRequest request) {
		Owner owner = getOwnerEntityById(ownerId);
		Pet existingPet = owner.getPet(petId);
		if (existingPet == null) {
			throw new ResourceNotFoundException(
					"Pet with id " + petId + " not found for owner with id " + ownerId + ".");
		}

		String normalizedName = request.name().strip();
		Pet duplicateCandidate = owner.getPet(normalizedName, false);
		if (duplicateCandidate != null && !Objects.equals(duplicateCandidate.getId(), petId)) {
			throw new DuplicateResourceException(
					"Pet with name '" + normalizedName + "' already exists for owner with id " + ownerId);
		}

		PetType petType = findPetTypeOrThrow(request.typeId());

		existingPet.setName(normalizedName);
		existingPet.setBirthDate(request.birthDate());
		existingPet.setType(petType);

		try {
			this.owners.saveAndFlush(owner);
		}
		catch (DataIntegrityViolationException ex) {
			if (isDuplicatePetNameViolation(ex)) {
				throw new DuplicateResourceException(
						"Pet with name '" + normalizedName + "' already exists for owner with id " + ownerId, ex);
			}
			throw ex;
		}

		return this.mapper.toPetResponse(existingPet, owner.getId());
	}

	private PetType findPetTypeOrThrow(Integer typeId) {
		return this.petTypes.findById(typeId).orElseThrow(() -> new ResourceNotFoundException("PetType", typeId));
	}

	private boolean isDuplicatePetNameViolation(DataIntegrityViolationException ex) {
		String message = ex.getMessage();
		return message != null && message.toLowerCase().contains("unique_owner_pet_name");
	}

}

