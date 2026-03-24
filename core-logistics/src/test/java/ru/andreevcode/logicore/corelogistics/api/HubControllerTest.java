package ru.andreevcode.logicore.corelogistics.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.andreevcode.logicore.corelogistics.data.RequestChangeCapacityDto;
import ru.andreevcode.logicore.corelogistics.data.RequestHubDto;
import ru.andreevcode.logicore.corelogistics.data.ResponseHubDto;
import ru.andreevcode.logicore.corelogistics.service.TransportHubService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class HubControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TransportHubService transportHubService;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16");

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("TRUNCATE TABLE logistics.transport_hub RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldGetAllPreExistedWith200() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO logistics.transport_hub (name,capacity, code) 
                VALUES 
                    ('test-hub-1', 101, 'hub-1'),
                    ('test-hub-2', 102, 'hub-2');
                """);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/hubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("test-hub-1"))
                .andExpect(jsonPath("$[0].capacity").value(101))
                .andExpect(jsonPath("$[0].code").value("hub-1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("test-hub-2"))
                .andExpect(jsonPath("$[1].capacity").value(102))
                .andExpect(jsonPath("$[1].code").value("hub-2"));
    }

    @Test
    void shouldPostOneWith201() throws Exception {
        var newRequestHubDto = new RequestHubDto("test-hub-3", 103, "hub-3");
        var expectedResponseHubDto = new ResponseHubDto(1L, "test-hub-3", 103, "hub-3");

        String jsonResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/hubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRequestHubDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var actualHub = objectMapper
                .readValue(jsonResponse, new TypeReference<ResponseHubDto>() {
                });
        assertThat(actualHub).isEqualTo(expectedResponseHubDto);
    }

    @Test
    void shouldPostOneWithNotValidParamsAndGet400WithErrors() throws Exception {
        var newRequestHubDto = new RequestHubDto("", -103, "");

        String jsonResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/hubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRequestHubDto)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var actualErrorsMap = objectMapper
                .readValue(jsonResponse, new TypeReference<Map<String, String>>() {
                });
        assertThat(actualErrorsMap).containsExactlyInAnyOrderEntriesOf(Map.of(
                "name", "must not be blank",
                "capacity", "must be greater than 0",
                "code", "must not be blank"
        ));

    }

    @Test
    void shouldGetExistedByIdWith200() throws Exception {
        jdbcTemplate.update("""
                    INSERT INTO logistics.transport_hub(name, capacity, code) VALUES ('test-hub-1', 101, 'hub-1');
                """);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/hubs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("test-hub-1"))
                .andExpect(jsonPath("$.capacity").value(101))
                .andExpect(jsonPath("$.code").value("hub-1"));
    }

    @Test
    void shouldGetNotExistedByIdWith404() throws Exception {
        jdbcTemplate.update("""
                    INSERT INTO logistics.transport_hub(name, capacity, code) VALUES ('test-hub-1', 101, 'hub-1');
                """);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/hubs/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("No transport hub found for id=2"));
    }

    @Test
    void shouldChangeCapacityWith200() throws Exception {
        var capacityChangeDto = new RequestChangeCapacityDto(-30);
        jdbcTemplate.update("""
                    INSERT INTO logistics.transport_hub(name, capacity, code) VALUES ('test-hub-1', 30, 'hub-1');
                """);

        var expectedHub = new ResponseHubDto(1L, "test-hub-1", 0, "hub-1");
        String jsonResponse = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/hubs/1/capacity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(capacityChangeDto))
                )
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var actualHub = objectMapper
                .readValue(jsonResponse, new TypeReference<ResponseHubDto>() {
                });
        assertThat(actualHub).isEqualTo(expectedHub);
    }

    @Test
    void shouldChangeForTooLowCapacityWith409() throws Exception {
        var capacityChangeDto = new RequestChangeCapacityDto(-30);
        jdbcTemplate.update("""
                    INSERT INTO logistics.transport_hub(name, capacity, code) VALUES ('test-hub-1', 10, 'hub-1');
                """);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/hubs/1/capacity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(capacityChangeDto))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$").value("Not enough capacity(10) for request(-30) at hub id=1, version=0"));
    }
}