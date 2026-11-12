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
package org.springframework.samples.petclinic.vet.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.dto.PageResponse;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.vet.dto.VetResponse;
import org.springframework.samples.petclinic.vet.mapper.VetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application and domain service for querying Veterinarians and their specialties.
 */
@Service
@Transactional(readOnly = true)
public class VetService {

	private final VetRepository vetRepository;

	private final VetMapper mapper;

	public VetService(VetRepository vetRepository, VetMapper mapper) {
		this.vetRepository = vetRepository;
		this.mapper = mapper;
	}

	public PageResponse<VetResponse> findPaginated(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page index must not be less than zero");
		}
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("Page size must be between 1 and 100");
		}
		Pageable pageable = PageRequest.of(page, size);
		Page<VetResponse> vetPage = this.vetRepository.findAll(pageable).map(this.mapper::toVetResponse);
		return PageResponse.from(vetPage);
	}

	public List<VetResponse> findAll() {
		return this.vetRepository.findAll().stream().map(this.mapper::toVetResponse).toList();
	}

}

