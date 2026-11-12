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
package org.springframework.samples.petclinic.visit;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.owner.mapper.OwnerPetMapper;
import org.springframework.samples.petclinic.owner.service.OwnerPetService;
import org.springframework.samples.petclinic.system.exception.GlobalExceptionHandler;
import org.springframework.samples.petclinic.visit.api.VisitRestController;
import org.springframework.samples.petclinic.visit.mapper.VisitMapper;
import org.springframework.samples.petclinic.visit.service.VisitService;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link VisitRestController}.
 */
@WebMvcTest(VisitRestController.class)
@Import({ VisitService.class, VisitMapper.class, OwnerPetService.class, OwnerPetMapper.class,
		GlobalExceptionHandler.class })
@DisabledInNativeImage
@DisabledInAotMode
class VisitRestControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository petTypes;

	@BeforeEach
	void init() {
		Owner owner = new Owner();
		owner.setId(TEST_OWNER_ID);
		owner.setFirstName("George");
		owner.setLastName("Franklin");

		Pet pet = new Pet();
		pet.setId(TEST_PET_ID);
		pet.setName("Leo");

		Visit existingVisit = new Visit();
		existingVisit.setId(5);
		existingVisit.setDate(LocalDate.of(2024, 1, 15));
		existingVisit.setDescription("Initial checkup");
		pet.addVisit(existingVisit);

		owner.addPet(pet);
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));
	}

	@Test
	void shouldListVisitsForPet() throws Exception {
		mockMvc
			.perform(get("/api/v1/owners/{ownerId}/pets/{petId}/visits", TEST_OWNER_ID, TEST_PET_ID)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(5))
			.andExpect(jsonPath("$[0].description").value("Initial checkup"));
	}

	@Test
	void shouldCreateVisitSuccessfully() throws Exception {
		given(this.owners.saveAndFlush(any(Owner.class))).willAnswer(invocation -> {
			Owner savedOwner = invocation.getArgument(0);
			Pet pet = savedOwner.getPet(TEST_PET_ID);
			pet.getVisits().forEach(v -> {
				if (v.getId() == null) {
					v.setId(77);
				}
			});
			return savedOwner;
		});

		String futureDate = LocalDate.now().plusDays(5).toString();
		String payload = """
				{
					"date": "%s",
					"description": "Rabies vaccination"
				}
				""".formatted(futureDate);

		mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets/{petId}/visits", TEST_OWNER_ID, TEST_PET_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(77))
			.andExpect(jsonPath("$.date").value(futureDate))
			.andExpect(jsonPath("$.description").value("Rabies vaccination"));
	}

	@Test
	void shouldRejectVisitWhenDescriptionIsBlank() throws Exception {
		String futureDate = LocalDate.now().plusDays(2).toString();
		String payload = """
				{
					"date": "%s",
					"description": "   "
				}
				""".formatted(futureDate);

		mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets/{petId}/visits", TEST_OWNER_ID, TEST_PET_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.description").exists());
	}

	@Test
	void shouldRejectVisitWhenDateIsNotInFuture() throws Exception {
		String today = LocalDate.now().toString();
		String payload = """
				{
					"date": "%s",
					"description": "Urgent check"
				}
				""".formatted(today);

		mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets/{petId}/visits", TEST_OWNER_ID, TEST_PET_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.date").exists());
	}

	@Test
	void shouldRejectVisitWhenDescriptionExceedsMaxLength() throws Exception {
		String futureDate = LocalDate.now().plusDays(1).toString();
		String payload = """
				{
					"date": "%s",
					"description": "%s"
				}
				""".formatted(futureDate, "a".repeat(256));

		mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets/{petId}/visits", TEST_OWNER_ID, TEST_PET_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.description").exists());
	}

}

