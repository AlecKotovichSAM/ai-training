package com.alec.aitraining;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alec.aitraining.domain.AuditEvent;
import com.alec.aitraining.domain.Outcome;
import com.alec.aitraining.dto.AuditEventResponse;
import com.alec.aitraining.dto.CreateAuditEventRequest;
import com.alec.aitraining.repository.AuditEventRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuditEventIntegrationTest {

	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	AuditEventRepository repository;
	@Autowired
	JdbcTemplate jdbcTemplate;

	// No cleanup in @BeforeEach - audit data should never be deleted
	// Tests use unique data to avoid conflicts

	// ─── POST /audit-events ───────────────────────────────────────────────────

	@Nested
	@DisplayName("POST /audit-events")
	class CreateAuditEvent {

		@Test
		@DisplayName("returns 201 and persists the event")
		void happyPath() throws Exception {
			// Use unique actor to avoid conflicts with other tests
			String uniqueActor = "user:" + System.currentTimeMillis();
			CreateAuditEventRequest req = new CreateAuditEventRequest(
					uniqueActor,
					"invoice.updated",
					"invoice:777",
					Outcome.SUCCESS,
					Map.of("ip", "10.0.0.1"));

			MvcResult result = mvc.perform(post("/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.timestamp").isNotEmpty())
				.andExpect(jsonPath("$.actor").value(uniqueActor))
				.andExpect(jsonPath("$.action").value("invoice.updated"))
				.andExpect(jsonPath("$.outcome").value("SUCCESS"))
				.andExpect(jsonPath("$.eventHash").isNotEmpty())
				.andReturn();

			AuditEventResponse resp = objectMapper.readValue(result.getResponse()
				.getContentAsString(), AuditEventResponse.class);

			assertThat(repository.findById(resp.id())).isPresent();
		}

		@Test
		@DisplayName("returns 400 when actor is blank")
		void missingActor() throws Exception {
			CreateAuditEventRequest req = new CreateAuditEventRequest("", "invoice.updated", "invoice:777",
					Outcome.SUCCESS, null);

			mvc.perform(post("/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").isNotEmpty());
		}

		@Test
		@DisplayName("timestamp is always set by the server, not by caller")
		void timestampSetByServer() throws Exception {
			CreateAuditEventRequest req = new CreateAuditEventRequest(
					"svc:payments", "payment.processed", "payment:99", Outcome.SUCCESS, null);

			MvcResult result = mvc.perform(post("/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andReturn();

			AuditEventResponse resp = objectMapper.readValue(result.getResponse()
				.getContentAsString(), AuditEventResponse.class);

			assertThat(resp.timestamp()).isNotNull();
		}

		@Test
		@DisplayName("hash chain — second event references first event hash")
		void hashChainLinks() throws Exception {
			CreateAuditEventRequest first = new CreateAuditEventRequest("user:1", "user.login", null, Outcome.SUCCESS,
					null);
			CreateAuditEventRequest second = new CreateAuditEventRequest(
					"user:1", "resource.updated", "project:10", Outcome.SUCCESS, null);

			AuditEventResponse r1 = objectMapper.readValue(mvc.perform(post("/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(first)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString(), AuditEventResponse.class);

			AuditEventResponse r2 = objectMapper.readValue(mvc.perform(post("/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(second)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString(), AuditEventResponse.class);

			assertThat(r2.previousHash()).isEqualTo(r1.eventHash());
		}
	}

	// ─── GET /audit-events ────────────────────────────────────────────────────

	@Nested
	@DisplayName("GET /audit-events")
	class SearchAuditEvents {

		@BeforeEach
		void seed() throws Exception {
			// Use unique actors to avoid conflicts with other tests
			String timestamp = String.valueOf(System.currentTimeMillis());
			postEvent("search-user:" + timestamp, "user.login", null, Outcome.SUCCESS);
			postEvent("search-user:" + timestamp, "project.deleted", "project:5-" + timestamp, Outcome.DENIED);
			postEvent("search-svc:" + timestamp, "payment.processed", "payment:1-" + timestamp, Outcome.SUCCESS);
		}

		@Test
		@DisplayName("returns all events when no filter given")
		void noFilter() throws Exception {
			mvc.perform(get("/audit-events"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").exists());
		}

		@Test
		@DisplayName("filters by actor")
		void filterByActor() throws Exception {
			String timestamp = String.valueOf(System.currentTimeMillis());
			String uniqueActor = "filter-test-user:" + timestamp;

			// Create test event with unique actor
			CreateAuditEventRequest req = new CreateAuditEventRequest(
					uniqueActor, "test.action", "test:resource", Outcome.SUCCESS, null);
			mvc.perform(post("/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)));

			// Test filter works
			mvc.perform(get("/audit-events").param("actor", uniqueActor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
		}

		@Test
		@DisplayName("filters by outcome")
		void filterByOutcome() throws Exception {
			mvc.perform(get("/audit-events").param("outcome", "SUCCESS"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").exists())
				.andExpect(jsonPath("$.content").isArray());
		}

		@Test
		@DisplayName("filters by resource")
		void filterByResource() throws Exception {
			String timestamp = String.valueOf(System.currentTimeMillis());
			String uniqueResource = "test-resource:" + timestamp;

			// Create test event with unique resource
			CreateAuditEventRequest req = new CreateAuditEventRequest(
					"test-user:" + timestamp, "test.action", uniqueResource, Outcome.SUCCESS, null);
			mvc.perform(post("/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)));

			// Test filter works
			mvc.perform(get("/audit-events").param("resource", uniqueResource))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));
		}

		@Test
		@DisplayName("filters by time range (from/to)")
		void filterByTimeRange() throws Exception {
			String uniqueActor = "time-range-user:" + System.currentTimeMillis();

			postEventAndRead(uniqueActor, "event.first", "resource:1", Outcome.SUCCESS);
			Thread.sleep(20);
			AuditEventResponse second = postEventAndRead(uniqueActor, "event.second", "resource:2", Outcome.SUCCESS);

			Instant from = second.timestamp().minusNanos(1);
			Instant to = second.timestamp().plusNanos(1);

			mvc.perform(get("/audit-events")
				.param("actor", uniqueActor)
				.param("from", from.toString())
				.param("to", to.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(second.id().toString()))
				.andExpect(jsonPath("$.content[0].action").value("event.second"));
		}

		private void postEvent(String actor, String action, String resource, Outcome outcome)
				throws Exception {
			CreateAuditEventRequest req = new CreateAuditEventRequest(actor, action, resource, outcome, null);
			mvc.perform(post("/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)));
		}

		private AuditEventResponse postEventAndRead(
				String actor, String action, String resource, Outcome outcome) throws Exception {
			CreateAuditEventRequest req = new CreateAuditEventRequest(actor, action, resource, outcome, null);
			MvcResult result = mvc.perform(post("/audit-events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isCreated())
				.andReturn();
			return objectMapper.readValue(result.getResponse().getContentAsString(), AuditEventResponse.class);
		}
	}

	@Nested
	@DisplayName("append-only database invariants")
	class AppendOnlyInvariants {

		@Test
		@DisplayName("database rejects UPDATE and DELETE on audit_events")
		void dbRejectsUpdateAndDelete() {
			AuditEvent saved = repository.save(AuditEvent.builder()
				.timestamp(Instant.now())
				.actor("append-only-user:" + System.currentTimeMillis())
				.action("created")
				.resource("resource:append-only")
				.outcome(Outcome.SUCCESS)
				.previousHash(null)
				.eventHash("hash-" + UUID.randomUUID().toString().replace("-", ""))
				.build());

			assertThatThrownBy(() -> jdbcTemplate
				.update("UPDATE audit_events SET action = ? WHERE id = ?", "updated", saved.getId()))
				.isInstanceOf(DataAccessException.class);

			assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_events WHERE id = ?", saved.getId()))
				.isInstanceOf(DataAccessException.class);
		}
	}
}
