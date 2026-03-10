package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.AppointmentService;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.SlotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AppointmentController {
    private final SlotService slotService;
    private final AppointmentService appointmentService;

    public AppointmentController(SlotService slotService, AppointmentService appointmentService) {
        this.slotService = slotService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/slots")
    public String viewSlots(Model model) {
        model.addAttribute("slots", slotService.getOpenSlotsWithProviderNames());
        return "slots"; // refers to templates/slots.html
    }

    // Displays the Booking Form
    @GetMapping("/book")
    public String showBookingForm(@RequestParam Integer slotId, Model model) {
        model.addAttribute("slotId", slotId);
        // In a real app, fetch the slot details here
        return "book";
    }

    // Handles the Form Submission
    @PostMapping("/book")
    public String processBooking(
            @RequestParam Integer slotId,
            @RequestParam String customerName,
            @RequestParam String customerEmail
    ) {
        AppointmentEntity appt = appointmentService.book(slotId, customerName, customerEmail);
        return "redirect:/confirmation?appointmentId=" + appt.appointmentId();
    }

    @GetMapping("/confirmation")
    public String showConfirmation(@RequestParam(required = false) Integer appointmentId, Model model) {
        if (appointmentId != null) {
            model.addAttribute("appointment", appointmentService.getById(appointmentId));
        }
        return "confirmation";
    }

    @GetMapping("/appointments")
    public String listAppointments(Model model) {
        model.addAttribute("appointments", appointmentService.listAll());
        return "appointments";
    }
}
