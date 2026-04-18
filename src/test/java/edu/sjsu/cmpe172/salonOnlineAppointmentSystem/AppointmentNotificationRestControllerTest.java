package edu.sjsu.cmpe172.salonOnlineAppointmentSystem;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller.AppointmentNotificationRestController;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationResponse;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.BookingConfirmationNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentNotificationRestControllerTest {

    private final FakeBookingConfirmationNotificationService confirmationNotificationService =
            new FakeBookingConfirmationNotificationService();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AppointmentNotificationRestController(confirmationNotificationService))
            .build();

    @Test
    void sendConfirmationReturnsVendorResponse() throws Exception {
        confirmationNotificationService.response =
                new BookingConfirmationNotificationResponse("ext-1-abcdef12", "ACCEPTED");
        confirmationNotificationService.error = null;

        mockMvc.perform(post("/api/appointments/1/confirmation-notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value("ext-1-abcdef12"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void sendConfirmationNotFound() throws Exception {
        confirmationNotificationService.response = null;
        confirmationNotificationService.error = new IllegalArgumentException("Appointment not found: 99");

        mockMvc.perform(post("/api/appointments/99/confirmation-notification"))
                .andExpect(status().isNotFound());
    }

    private static final class FakeBookingConfirmationNotificationService
            extends BookingConfirmationNotificationService {
        private BookingConfirmationNotificationResponse response;
        private IllegalArgumentException error;

        private FakeBookingConfirmationNotificationService() {
            super(null, null, null, null);
        }

        @Override
        public BookingConfirmationNotificationResponse dispatch(Integer appointmentId) {
            if (error != null) {
                throw error;
            }
            return response;
        }
    }
}
