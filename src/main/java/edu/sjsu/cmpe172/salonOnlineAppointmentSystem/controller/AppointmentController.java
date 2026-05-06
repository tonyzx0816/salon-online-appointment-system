package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationResponse;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.AppointmentService;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.BookingConfirmationNotificationService;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.ChatService;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.SlotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class AppointmentController {
    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final SlotService slotService;
    private final AppointmentService appointmentService;
    private final BookingConfirmationNotificationService confirmationNotificationService;
    private final ChatService chatService;
    private final UserRepository userRepository;

    public AppointmentController(
            SlotService slotService,
            AppointmentService appointmentService,
            BookingConfirmationNotificationService confirmationNotificationService,
            ChatService chatService,
            UserRepository userRepository
    ) {
        this.slotService = slotService;
        this.appointmentService = appointmentService;
        this.confirmationNotificationService = confirmationNotificationService;
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @GetMapping("/slots")
    public String viewSlots(Model model) {
        model.addAttribute("slots", slotService.getOpenSlotsWithProviderNames());
        return "slots";
    }

    @GetMapping("/book")
    public String showBookingForm(@RequestParam Integer slotId, Model model) {
        model.addAttribute("slotId", slotId);
        return "book";
    }

    @PostMapping("/book")
    public String processBooking(
            @RequestParam Integer slotId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UserEntity currentUser = requireCurrentUser(authentication);
            AppointmentEntity appt = appointmentService.book(slotId, currentUser.name(), currentUser.email());
            chatService.activateChatForBookedAppointment(appt.appointmentId(), currentUser.userId());
            try {
                BookingConfirmationNotificationResponse notificationResponse =
                        confirmationNotificationService.dispatch(appt.appointmentId());
                return "redirect:/confirmation?appointmentId=" + appt.appointmentId()
                        + "&notificationStatus=" + notificationResponse.status()
                        + "&notificationId=" + notificationResponse.notificationId();
            } catch (RuntimeException ex) {
                log.error("booking.notification.failed appointmentId={}", appt.appointmentId(), ex);
                String reason = ex.getMessage();
                if (reason == null || reason.isBlank()) {
                    reason = ex.getClass().getSimpleName();
                } else if (reason.length() > 500) {
                    reason = reason.substring(0, 500) + "…";
                }
                redirectAttributes.addFlashAttribute("notificationFailureReason", reason);
                return "redirect:/confirmation?appointmentId=" + appt.appointmentId()
                        + "&notificationStatus=FAILED";
            }
        } catch (IllegalStateException ex) {
            model.addAttribute("slotId", slotId);
            model.addAttribute("errorMessage", ex.getMessage());
            return "unavailable";
        }
    }

    @GetMapping("/confirmation")
    public String showConfirmation(
            @RequestParam(required = false) Integer appointmentId,
            @RequestParam(required = false) String notificationStatus,
            @RequestParam(required = false) String notificationId,
            Model model
    ) {
        if (appointmentId != null) {
            AppointmentEntity appt = appointmentService.getById(appointmentId);
            model.addAttribute("appointment", appt);
            userRepository.findById(appt.customerId())
                    .ifPresent(u -> model.addAttribute("customerAccountEmail", u.email()));
        }
        model.addAttribute("notificationStatus", notificationStatus);
        model.addAttribute("notificationId", notificationId);
        return "confirmation";
    }

    @GetMapping("/appointments")
    public String listAppointments(Authentication authentication, Model model) {
        UserEntity user = requireCurrentUser(authentication);
        model.addAttribute("rows", appointmentService.listHistoryForUser(user));
        model.addAttribute("customerView", user.role() == UserEntity.Role.CUSTOMER);
        return "appointments";
    }

    @PostMapping("/appointments/cancel")
    public String cancelAppointment(
            @RequestParam Integer appointmentId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UserEntity user = requireCurrentUser(authentication);
            if (user.role() != UserEntity.Role.CUSTOMER) {
                redirectAttributes.addFlashAttribute("error", "Only customers can cancel bookings.");
                return "redirect:/appointments";
            }
            appointmentService.cancelForCustomer(appointmentId, user.userId());
            redirectAttributes.addFlashAttribute("message", "Your appointment was cancelled.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/appointments";
    }

    @GetMapping("/appointments/reschedule")
    public String rescheduleForm(
            @RequestParam Integer appointmentId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        UserEntity user = requireCurrentUser(authentication);
        if (user.role() != UserEntity.Role.CUSTOMER) {
            redirectAttributes.addFlashAttribute("error", "Only customers can reschedule online.");
            return "redirect:/appointments";
        }
        AppointmentEntity appt = appointmentService.getById(appointmentId);
        if (!appt.customerId().equals(user.userId())) {
            redirectAttributes.addFlashAttribute("error", "That appointment is not yours.");
            return "redirect:/appointments";
        }
        LocalDateTime now = LocalDateTime.now();
        if (appt.status() != AppointmentEntity.Status.BOOKED || !appt.startTime().isAfter(now)) {
            redirectAttributes.addFlashAttribute("error", "This booking cannot be rescheduled.");
            return "redirect:/appointments";
        }
        model.addAttribute("appointment", appt);
        model.addAttribute("slots", slotService.getOpenFutureSlotsForProvider(appt.providerId()));
        return "reschedule";
    }

    @PostMapping("/appointments/reschedule")
    public String rescheduleSubmit(
            @RequestParam Integer appointmentId,
            @RequestParam Integer newSlotId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UserEntity user = requireCurrentUser(authentication);
            if (user.role() != UserEntity.Role.CUSTOMER) {
                redirectAttributes.addFlashAttribute("error", "Only customers can reschedule.");
                return "redirect:/appointments";
            }
            AppointmentEntity updated = appointmentService.rescheduleForCustomer(
                    appointmentId,
                    newSlotId,
                    user.userId()
            );
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Your appointment was moved to " + updated.startTime() + "."
            );
        } catch (OptimisticLockingFailureException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "That time was just taken. Please pick another slot."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/appointments";
    }

    private UserEntity requireCurrentUser(Authentication authentication) {
        String login = authentication.getName();
        return userRepository.findByEmail(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .orElseThrow(() -> new IllegalArgumentException("Current user not found."));
    }
}
