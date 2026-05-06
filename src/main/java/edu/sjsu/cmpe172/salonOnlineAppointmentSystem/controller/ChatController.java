package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.dto.ChatBookingOption;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.dto.ChatThreadResponse;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ConversationEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.MessageEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final UserRepository userRepository;

    public ChatController(ChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    /**
     * Bookings that have chat available, newest first. Labels are safe to show in a dropdown (no raw ids).
     */
    @GetMapping("/booking-options")
    public List<ChatBookingOption> listBookingOptions(Authentication authentication) {
        return chatService.listBookingOptions(getCurrentUser(authentication));
    }

    /**
     * Single chat thread for the signed-in user: most recent active booking, or a specific booking when
     * {@code appointmentId} is supplied (for example from the confirmation page link — users never type this).
     */
    @GetMapping("/thread")
    public ChatThreadResponse getThread(
            Authentication authentication,
            @RequestParam(required = false) Integer appointmentId
    ) {
        return chatService.loadThread(getCurrentUser(authentication), appointmentId);
    }

    @PostMapping("/thread/messages")
    public ResponseEntity<Void> postThreadMessage(
            Authentication authentication,
            @RequestParam(required = false) Integer appointmentId,
            @RequestBody SendMessageRequest request
    ) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Message content is required.");
        }
        chatService.sendThreadMessage(getCurrentUser(authentication), appointmentId, request.content());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationEntity> createConversation(
            Authentication authentication,
            @RequestBody CreateConversationRequest request
    ) {
        Integer currentUserId = getCurrentUser(authentication).userId();
        ConversationEntity conversation = chatService.activateChatForBookedAppointment(request.appointmentId(), currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    @GetMapping("/appointments/booked")
    public List<AppointmentEntity> listMyBookedAppointments(Authentication authentication) {
        return chatService.listMyBookedAppointments(getCurrentUser(authentication).userId());
    }

    @GetMapping("/conversations")
    public List<ConversationEntity> listMyConversations(Authentication authentication) {
        return chatService.listMyConversations(getCurrentUser(authentication).userId());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageEntity> listMessages(
            Authentication authentication,
            @PathVariable Integer conversationId
    ) {
        return chatService.listMessages(conversationId, getCurrentUser(authentication).userId());
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageEntity> sendMessage(
            Authentication authentication,
            @PathVariable Integer conversationId,
            @RequestBody SendMessageRequest request
    ) {
        MessageEntity message = chatService.sendMessage(
                conversationId,
                getCurrentUser(authentication).userId(),
                request.content()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    private UserEntity getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found."));
    }

    public record CreateConversationRequest(
            Integer appointmentId
    ) {
    }

    public record SendMessageRequest(String content) {
    }
}
