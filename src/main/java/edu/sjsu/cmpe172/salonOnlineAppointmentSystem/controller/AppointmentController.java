package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.SlotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AppointmentController {
    private final SlotService slotService;

    public AppointmentController(SlotService slotService) {
        this.slotService = slotService;
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
    public String processBooking(@RequestParam Integer slotId, @RequestParam String customerName) {
        // Logic to save the appointment would go here in the Service layer
        System.out.println("Booking slot " + slotId + " for " + customerName);
        return "redirect:/confirmation";
    }

    @GetMapping("/confirmation")
    public String showConfirmation() {
        return "confirmation";
    }
}
