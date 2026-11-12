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

package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "logging.level.sql=DEBUG")
@AutoConfigureMockMvc
public class PetClinicIntegrationTests {

	@Autowired
	private VetRepository vets;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void findAll() {
		this.vets.findAll();
		this.vets.findAll(); // served from cache
	}

	@Test
	void ownerDetails() throws Exception {
		this.mockMvc.perform(get("/api/v1/owners/1").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.lastName").value("Franklin"));
	}

	@Test
	void ownerList() throws Exception {
		this.mockMvc.perform(get("/api/v1/owners").param("lastName", "").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.totalElements").value(10));
	}

	@Test
	void vetsList() throws Exception {
		this.mockMvc.perform(get("/api/v1/vets").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.totalElements").value(6));
	}

	public static void main(String[] args) {
		SpringApplication.run(PetClinicApplication.class, "--spring.docker.compose.lifecycle-management=NONE");
	}

}
