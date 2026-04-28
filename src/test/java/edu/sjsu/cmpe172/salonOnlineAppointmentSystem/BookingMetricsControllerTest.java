package edu.sjsu.cmpe172.salonOnlineAppointmentSystem;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller.BookingMetricsController;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingMetricsControllerTest {

    @Test
    void bookingSummaryReturnsAggregatedBookingMetrics() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        Counter successCounter = meterRegistry.counter("booking_requests_total", "result", "success");
        Counter failureCounter = meterRegistry.counter("booking_requests_total", "result", "failure");
        Timer bookingLatency = Timer.builder("booking_latency_ms").register(meterRegistry);

        successCounter.increment(3.0);
        failureCounter.increment();
        bookingLatency.record(100, TimeUnit.MILLISECONDS);
        bookingLatency.record(200, TimeUnit.MILLISECONDS);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BookingMetricsController(meterRegistry)).build();

        mockMvc.perform(get("/metrics/booking-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings_success_total").value(3))
                .andExpect(jsonPath("$.bookings_failed_total").value(1))
                .andExpect(jsonPath("$.booking_latency_avg_ms").value(150.0))
                .andExpect(jsonPath("$.bookings_per_hour_note")
                        .value("Use rate(booking_requests_total{result=\"success\"}[1h]) in Prometheus/Grafana."));
    }
}
