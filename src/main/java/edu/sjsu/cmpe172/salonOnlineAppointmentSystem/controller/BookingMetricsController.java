package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class BookingMetricsController {
    private final MeterRegistry meterRegistry;

    public BookingMetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/booking-summary")
    public Map<String, Object> bookingSummary() {
        Counter success = meterRegistry.find("booking_requests_total")
                .tag("result", "success")
                .counter();
        Counter failure = meterRegistry.find("booking_requests_total")
                .tag("result", "failure")
                .counter();
        Timer latency = meterRegistry.find("booking_latency_ms").timer();

        double successCount = success == null ? 0.0 : success.count();
        double failureCount = failure == null ? 0.0 : failure.count();
        double avgLatencyMs = latency == null ? 0.0 : latency.mean(java.util.concurrent.TimeUnit.MILLISECONDS);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bookings_success_total", Math.round(successCount));
        body.put("bookings_failed_total", Math.round(failureCount));
        body.put("booking_latency_avg_ms", Math.round(avgLatencyMs * 100.0) / 100.0);
        body.put("bookings_per_hour_note", "Use rate(booking_requests_total{result=\"success\"}[1h]) in Prometheus/Grafana.");
        return body;
    }
}
