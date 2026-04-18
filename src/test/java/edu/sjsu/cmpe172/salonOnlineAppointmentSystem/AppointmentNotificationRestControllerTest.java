package edu.sjsu.cmpe172.salonOnlineAppointmentSystem;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller.AppointmentNotificationRestController;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationResponse;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.BookingConfirmationNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppointmentNotificationRestController.class)
class AppointmentNotificationRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingConfirmationNotificationService confirmationNotificationService;

    @Test
    void sendConfirmationReturnsVendorResponse() throws Exception {
        when(confirmationNotificationService.dispatch(anyInt()))
                .thenReturn(new BookingConfirmationNotificationResponse("ext-1-abcdef12", "ACCEPTED"));

        mockMvc.perform(post("/api/appointments/1/confirmation-notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notification_id").value("ext-1-abcdef12"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void sendConfirmationNotFound() throws Exception {
        when(confirmationNotificationService.dispatch(anyInt()))
                .thenThrow(new IllegalArgumentException("Appointment not found: 99"));

        mockMvc.perform(post("/api/appointments/99/confirmation-notification"))
                .andExpect(status().isNotFound());
    }
}
