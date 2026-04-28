package edu.sjsu.cmpe172.salonOnlineAppointmentSystem;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {

    @Test
    void healthReturnsUpWhenDatabaseIsReachable() throws Exception {
        JdbcTemplate jdbcTemplate = new FakeJdbcTemplate(1, null);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(jdbcTemplate)).build();

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("salon-online-appointment-system"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void healthReturnsDegradedWhenDatabaseIsDown() throws Exception {
        JdbcTemplate jdbcTemplate = new FakeJdbcTemplate(null, new RuntimeException("database unavailable"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(jdbcTemplate)).build();

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.database").value("DOWN"))
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private static final class FakeJdbcTemplate extends JdbcTemplate {
        private final Integer result;
        private final RuntimeException error;

        private FakeJdbcTemplate(Integer result, RuntimeException error) {
            this.result = result;
            this.error = error;
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            if (error != null) {
                throw error;
            }
            return requiredType.cast(result);
        }
    }
}
