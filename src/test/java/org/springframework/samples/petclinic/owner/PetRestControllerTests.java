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
package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.owner.api.PetRestController;
import org.springframework.samples.petclinic.owner.api.PetTypeRestController;
import org.springframework.samples.petclinic.owner.mapper.OwnerPetMapper;
import org.springframework.samples.petclinic.owner.service.OwnerPetService;
import org.springframework.samples.petclinic.system.exception.GlobalExceptionHandler;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link PetRestController} and {@link PetTypeRestController}.
 */
@WebMvcTest({ PetRestController.class, PetTypeRestController.class })
@Import({ OwnerPetService.class, OwnerPetMapper.class, GlobalExceptionHandler.class })
@DisabledInNativeImage
@DisabledInAotMode
class PetRestControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	private static final int TEST_TYPE_ID = 3;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository types;

	@BeforeEach
	void setup() {
		PetType hamster = new PetType();
		hamster.setId(TEST_TYPE_ID);
		hamster.setName("hamster");
		given(this.types.findPetTypes()).willReturn(List.of(hamster));
		given(this.types.findById(TEST_TYPE_ID)).willReturn(Optional.of(hamster));

		Owner owner = new Owner();
		owner.setId(TEST_OWNER_ID);
		owner.setFirstName("George");
		owner.setLastName("Franklin");

		Pet pet = new Pet();
		pet.setId(TEST_PET_ID);
		pet.setName("petty");
		pet.setBirthDate(LocalDate.of(2015, 2, 12));
		pet.setType(hamster);

		Pet dog = new Pet();
		dog.setId(TEST_PET_ID + 1);
		dog.setName("doggy");
		dog.setBirthDate(LocalDate.of(2016, 3, 10));
		dog.setType(hamster);

		owner.addPet(pet);
		owner.addPet(dog);
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));
	}

	@Test
	void shouldListAllPetTypes() throws Exception {
		mockMvc.perform(get("/api/v1/pet-types").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(TEST_TYPE_ID))
			.andExpect(jsonPath("$[0].name").value("hamster"));
	}

	@Test
	void shouldListPetsForOwner() throws Exception {
		mockMvc.perform(get("/api/v1/owners/{ownerId}/pets", TEST_OWNER_ID).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].name").value("petty"));
	}

	@Test
	void shouldGetPetById() throws Exception {
		mockMvc
			.perform(get("/api/v1/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, TEST_PET_ID)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TEST_PET_ID))
			.andExpect(jsonPath("$.name").value("petty"));
	}

	@Test
	void shouldCreatePetSuccessfully() throws Exception {
		given(this.owners.saveAndFlush(any(Owner.class))).willAnswer(invocation -> {
			Owner owner = invocation.getArgument(0);
			Pet added = owner.getPet("Betty", false);
			if (added != null) {
				added.setId(99);
			}
			return owner;
		});

		String payload = """
				{
					"name": "Betty",
					"birthDate": "2015-02-12",
					"typeId": 3
				}
				""";

		mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets", TEST_OWNER_ID).contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(99))
			.andExpect(jsonPath("$.name").value("Betty"))
			.andExpect(jsonPath("$.type.name").value("hamster"));
	}

	@Test
	void shouldRejectCreatePetWithBlankNameOrFutureBirthDate() throws Exception {
		String futureDate = LocalDate.now().plusMonths(1).toString();
		String payload = """
				{
					"name": "   ",
					"birthDate": "%s",
					"typeId": 3
				}
				""".formatted(futureDate);

		mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets", TEST_OWNER_ID).contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.birthDate").exists());
	}

	@Test
	void shouldReturn409ConflictWhenCreatingDuplicatePetNameForSameOwner() throws Exception {
		String payload = """
				{
					"name": "petty",
					"birthDate": "2015-02-12",
					"typeId": 3
				}
				""";

		mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets", TEST_OWNER_ID).contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void shouldReturn409ConflictOnDataIntegrityViolation() throws Exception {
		given(this.owners.saveAndFlush(any(Owner.class)))
			.willThrow(new DataIntegrityViolationException("Duplicate key: unique_owner_pet_name"));

		String payload = """
				{
					"name": "Betty",
					"birthDate": "2015-02-12",
					"typeId": 3
				}
				""";

		mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets", TEST_OWNER_ID).contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void shouldUpdatePetSuccessfully() throws Exception {
		String payload = """
				{
					"name": "Betty",
					"birthDate": "2015-02-12",
					"typeId": 3
				}
				""";

		mockMvc
			.perform(put("/api/v1/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, TEST_PET_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TEST_PET_ID))
			.andExpect(jsonPath("$.name").value("Betty"));
	}

	@Test
	void shouldReturn409WhenUpdatingPetToAnotherExistingPetName() throws Exception {
		String payload = """
				{
					"name": "petty",
					"birthDate": "2015-02-12",
					"typeId": 3
				}
				""";

		mockMvc
			.perform(put("/api/v1/owners/{ownerId}/pets/{petId}", TEST_OWNER_ID, TEST_PET_ID + 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
	}

}

