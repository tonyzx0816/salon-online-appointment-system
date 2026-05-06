package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.dto.ChatBookingOption;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.dto.ChatThreadResponse;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.dto.ChatThreadResponse.ChatMessageLine;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ConversationEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.MessageEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ProviderEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AppointmentRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ConversationRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.MessageRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ProviderRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ChatService {
    private static final DateTimeFormatter BOOKED_APPOINTMENT_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(Locale.getDefault(Locale.Category.FORMAT));
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProviderRepository providerRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ProviderRepository providerRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.providerRepository = providerRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    public ConversationEntity activateChatForBookedAppointment(Integer appointmentId, Integer requesterId) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("appointmentId is required.");
        }
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        if (appointment.status() != AppointmentEntity.Status.BOOKED) {
            throw new IllegalArgumentException("Chat is only available for BOOKED appointments.");
        }

        ProviderEntity provider = providerRepository.findById(appointment.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + appointment.providerId()));
        if (!isParticipantOnly(requesterId, appointment.customerId(), provider)) {
            throw new IllegalArgumentException("Only booked customer/provider can activate chat.");
        }

        return conversationRepository.findByAppointmentId(appointmentId).orElseGet(() ->
                conversationRepository.save(new ConversationEntity(
                        null,
                        appointment.customerId(),
                        appointment.providerId(),
                        appointmentId,
                        ConversationEntity.Status.OPEN,
                        LocalDateTime.now()
                ))
        );
    }

    /**
     * All active bookings the viewer can open a chat for, newest first (for picker UI).
     */
    public List<ChatBookingOption> listBookingOptions(UserEntity viewer) {
        List<AppointmentEntity> appts = switch (viewer.role()) {
            case CUSTOMER -> appointmentRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(
                    viewer.userId(),
                    AppointmentEntity.Status.BOOKED
            );
            case PROVIDER -> providerRepository.findByUserId(viewer.userId())
                    .map(p -> appointmentRepository.findByProviderIdAndStatusOrderByCreatedAtDesc(
                            p.providerId(),
                            AppointmentEntity.Status.BOOKED
                    ))
                    .orElse(List.of());
            default -> List.of();
        };
        return appts.stream().map(a -> toBookingOption(viewer, a)).toList();
    }

    private ChatBookingOption toBookingOption(UserEntity viewer, AppointmentEntity appointment) {
        String slot = appointment.startTime().format(BOOKED_APPOINTMENT_FORMAT);
        String label = switch (viewer.role()) {
            case CUSTOMER -> resolvePeerName(viewer.userId(), appointment) + " · " + slot;
            case PROVIDER -> userRepository.findById(appointment.customerId())
                    .map(UserEntity::name)
                    .orElse("Customer") + " · " + slot;
            default -> slot;
        };
        return new ChatBookingOption(appointment.appointmentId(), label);
    }

    /**
     * Loads the chat thread for the viewer: either a specific booked appointment (optional) or the most recently
     * created BOOKED appointment for this customer or provider account.
     */
    public ChatThreadResponse loadThread(UserEntity viewer, Integer focusAppointmentId) {
        ResolvedChat resolved = ensureConversation(viewer, focusAppointmentId);
        return buildThreadResponse(viewer.userId(), resolved);
    }

    public void sendThreadMessage(UserEntity viewer, Integer focusAppointmentId, String content) {
        ResolvedChat resolved = ensureConversation(viewer, focusAppointmentId);
        sendMessage(resolved.conversation().conversationId(), viewer.userId(), content);
    }

    private ResolvedChat ensureConversation(UserEntity viewer, Integer focusAppointmentId) {
        AppointmentEntity appointment = resolveFocusedOrLatestBooked(viewer, focusAppointmentId);
        ConversationEntity conversation = activateChatForBookedAppointment(
                appointment.appointmentId(),
                viewer.userId()
        );
        return new ResolvedChat(appointment, conversation);
    }

    private AppointmentEntity resolveFocusedOrLatestBooked(UserEntity user, Integer focusAppointmentId) {
        if (focusAppointmentId != null) {
            AppointmentEntity appointment = appointmentRepository.findById(focusAppointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
            if (appointment.status() != AppointmentEntity.Status.BOOKED) {
                throw new IllegalArgumentException("Chat is only available for active bookings.");
            }
            ProviderEntity provider = providerRepository.findById(appointment.providerId())
                    .orElseThrow(() -> new IllegalArgumentException("Provider not found."));
            if (!isParticipantOnly(user.userId(), appointment.customerId(), provider)) {
                throw new IllegalArgumentException("You cannot open chat for this appointment.");
            }
            return appointment;
        }

        return switch (user.role()) {
            case CUSTOMER -> appointmentRepository
                    .findByCustomerIdAndStatusOrderByCreatedAtDesc(user.userId(), AppointmentEntity.Status.BOOKED)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No active booking yet. Book an appointment, then return here to message your provider."
                    ));
            case PROVIDER -> {
                ProviderEntity provider = providerRepository.findByUserId(user.userId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "This account is not linked to a provider profile."
                        ));
                yield appointmentRepository
                        .findByProviderIdAndStatusOrderByCreatedAtDesc(
                                provider.providerId(),
                                AppointmentEntity.Status.BOOKED
                        )
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No customer bookings to reply to yet."
                        ));
            }
            default -> throw new IllegalArgumentException("Chat is only for customers and providers.");
        };
    }

    private ChatThreadResponse buildThreadResponse(int viewerUserId, ResolvedChat resolved) {
        AppointmentEntity appt = resolved.appointment();
        String peerName = resolvePeerName(viewerUserId, appt);
        String summary = "Booked: " + appt.startTime().format(BOOKED_APPOINTMENT_FORMAT)
                + " – " + appt.endTime().format(BOOKED_APPOINTMENT_FORMAT);

        List<MessageEntity> rows = messageRepository.findByConversationIdOrderBySentAtAsc(
                resolved.conversation().conversationId()
        );
        List<ChatMessageLine> lines = rows.stream()
                .map(m -> new ChatMessageLine(
                        m.content(),
                        m.sentAt() != null ? m.sentAt().toString() : "",
                        Objects.equals(m.senderId(), viewerUserId)
                ))
                .toList();

        String headline = Objects.equals(appt.customerId(), viewerUserId)
                ? "Chat with provider " + peerName
                : "Chat with customer " + peerName;

        return new ChatThreadResponse(headline, peerName, summary, lines);
    }

    private String resolvePeerName(int viewerUserId, AppointmentEntity appt) {
        if (Objects.equals(appt.customerId(), viewerUserId)) {
            return providerRepository.findById(appt.providerId())
                    .map(ProviderEntity::displayName)
                    .orElse("Your provider");
        }
        return userRepository.findById(appt.customerId())
                .map(UserEntity::name)
                .orElse("Customer");
    }

    private record ResolvedChat(AppointmentEntity appointment, ConversationEntity conversation) {
    }

    public List<ConversationEntity> listMyConversations(Integer currentUserId) {
        Integer providerId = providerRepository.findByUserId(currentUserId)
                .map(ProviderEntity::providerId)
                .orElse(-1);
        return conversationRepository.findByCustomerIdOrProviderIdOrderByCreatedAtDesc(currentUserId, providerId);
    }

    public List<AppointmentEntity> listMyBookedAppointments(Integer currentUserId) {
        List<AppointmentEntity> out = new ArrayList<>();
        out.addAll(appointmentRepository.findByCustomerIdOrderByStartTimeDesc(currentUserId).stream()
                .filter(a -> a.status() == AppointmentEntity.Status.BOOKED)
                .toList());
        providerRepository.findByUserId(currentUserId).ifPresent(provider ->
                out.addAll(appointmentRepository.findByProviderIdOrderByStartTimeDesc(provider.providerId()).stream()
                        .filter(a -> a.status() == AppointmentEntity.Status.BOOKED)
                        .toList())
        );
        return out;
    }

    public List<MessageEntity> listMessages(Integer conversationId, Integer currentUserId) {
        ConversationEntity conversation = getConversationOrThrow(conversationId);
        if (!isConversationParticipantOrAdmin(conversation, currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to view this conversation.");
        }
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    public MessageEntity sendMessage(Integer conversationId, Integer currentUserId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content is required.");
        }
        ConversationEntity conversation = getConversationOrThrow(conversationId);
        if (!isConversationParticipantOrAdmin(conversation, currentUserId)) {
            throw new IllegalArgumentException("You are not allowed to post to this conversation.");
        }

        return messageRepository.save(new MessageEntity(
                null,
                conversationId,
                currentUserId,
                content.trim(),
                LocalDateTime.now(),
                null
        ));
    }

    private ConversationEntity getConversationOrThrow(Integer conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
    }

    private boolean isConversationParticipantOrAdmin(ConversationEntity conversation, Integer userId) {
        ProviderEntity provider = providerRepository.findById(conversation.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + conversation.providerId()));
        return isParticipantOnly(userId, conversation.customerId(), provider);
    }

    private boolean isParticipantOnly(Integer userId, Integer customerId, ProviderEntity provider) {
        if (userId.equals(customerId)) {
            return true;
        }
        return provider.userId() != null && userId.equals(provider.userId());
    }
}
