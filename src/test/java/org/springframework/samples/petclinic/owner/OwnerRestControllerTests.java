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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.owner.api.OwnerRestController;
import org.springframework.samples.petclinic.owner.mapper.OwnerPetMapper;
import org.springframework.samples.petclinic.owner.service.OwnerPetService;
import org.springframework.samples.petclinic.system.exception.GlobalExceptionHandler;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link OwnerRestController}.
 */
@WebMvcTest(OwnerRestController.class)
@Import({ OwnerPetService.class, OwnerPetMapper.class, GlobalExceptionHandler.class })
@DisabledInNativeImage
@DisabledInAotMode
class OwnerRestControllerTests {

	private static final int TEST_OWNER_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository petTypes;

	private Owner george() {
		Owner george = new Owner();
		george.setId(TEST_OWNER_ID);
		george.setFirstName("George");
		george.setLastName("Franklin");
		george.setAddress("110 W. Liberty St.");
		george.setCity("Madison");
		george.setTelephone("6085551023");

		Pet max = new Pet();
		PetType dog = new PetType();
		dog.setId(2);
		dog.setName("dog");
		max.setType(dog);
		max.setName("Max");
		max.setBirthDate(LocalDate.of(2020, 1, 1));
		max.setId(1);

		Visit visit = new Visit();
		visit.setId(10);
		visit.setDate(LocalDate.of(2023, 5, 10));
		visit.setDescription("Annual checkup");
		max.addVisit(visit);

		george.addPet(max);
		return george;
	}

	@BeforeEach
	void setup() {
		Owner george = george();
		given(this.owners.findByLastNameStartingWith(eq("Franklin"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george)));
		given(this.owners.findByLastNameStartingWith(eq(""), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george)));
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(george));
	}

	@Test
	void shouldListOwnersPaginated() throws Exception {
		mockMvc.perform(get("/api/v1/owners").param("page", "0").param("size", "5").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(TEST_OWNER_ID))
			.andExpect(jsonPath("$.content[0].firstName").value("George"))
			.andExpect(jsonPath("$.content[0].lastName").value("Franklin"))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void shouldStripWhitespaceWhenSearchingByLastName() throws Exception {
		mockMvc.perform(get("/api/v1/owners").param("lastName", "  Franklin  ").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].lastName").value("Franklin"));

		verify(this.owners).findByLastNameStartingWith(eq("Franklin"), any(Pageable.class));
	}

	@Test
	void shouldReturnEmptyPageWhenNoOwnersMatchLastName() throws Exception {
		given(this.owners.findByLastNameStartingWith(eq("Unknown"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of()));

		mockMvc.perform(get("/api/v1/owners").param("lastName", "Unknown").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isEmpty())
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void shouldRejectNegativePageNumber() throws Exception {
		mockMvc.perform(get("/api/v1/owners").param("page", "-1").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void shouldGetOwnerById() throws Exception {
		mockMvc.perform(get("/api/v1/owners/{ownerId}", TEST_OWNER_ID).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TEST_OWNER_ID))
			.andExpect(jsonPath("$.firstName").value("George"))
			.andExpect(jsonPath("$.pets[0].name").value("Max"))
			.andExpect(jsonPath("$.pets[0].visits[0].description").value("Annual checkup"));
	}

	@Test
	void shouldReturn404WhenOwnerNotFound() throws Exception {
		given(this.owners.findById(999)).willReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/owners/999").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.title").value("Resource Not Found"));
	}

	@Test
	void shouldCreateOwnerSuccessfully() throws Exception {
		given(this.owners.save(any(Owner.class))).willAnswer(invocation -> {
			Owner saved = invocation.getArgument(0);
			saved.setId(42);
			return saved;
		});

		String payload = """
				{
					"firstName": "Joe",
					"lastName": "Bloggs",
					"address": "123 Caramel Street",
					"city": "London",
					"telephone": "1316761638"
				}
				""";

		mockMvc.perform(post("/api/v1/owners").contentType(MediaType.APPLICATION_JSON).content(payload))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/v1/owners/42"))
			.andExpect(jsonPath("$.id").value(42))
			.andExpect(jsonPath("$.firstName").value("Joe"))
			.andExpect(jsonPath("$.lastName").value("Bloggs"));
	}

	@Test
	void shouldRejectCreateOwnerWithValidationErrors() throws Exception {
		String invalidPayload = """
				{
					"firstName": "Joe",
					"lastName": "Bloggs",
					"address": "",
					"city": "London",
					"telephone": "invalid-phone"
				}
				""";

		mockMvc.perform(post("/api/v1/owners").contentType(MediaType.APPLICATION_JSON).content(invalidPayload))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.address").exists())
			.andExpect(jsonPath("$.errors.telephone").exists());
	}

	@Test
	void shouldUpdateOwnerSuccessfully() throws Exception {
		given(this.owners.save(any(Owner.class))).willAnswer(invocation -> invocation.getArgument(0));

		String payload = """
				{
					"firstName": "George",
					"lastName": "FranklinUpdated",
					"address": "110 W. Liberty St.",
					"city": "Madison",
					"telephone": "6085551023"
				}
				""";

		mockMvc
			.perform(put("/api/v1/owners/{ownerId}", TEST_OWNER_ID).contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TEST_OWNER_ID))
			.andExpect(jsonPath("$.lastName").value("FranklinUpdated"));
	}

}

