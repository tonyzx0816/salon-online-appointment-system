package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ProviderEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ServiceRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Sends booking confirmation over SMTP when {@link JavaMailSender} is available.
 * <p>
 * This bean is always registered; it resolves {@link JavaMailSender} lazily so it is not dropped by
 * {@code @ConditionalOnBean} ordering issues with component scanning vs. mail auto-configuration.
 * <p>
 * The <strong>recipient</strong> is always {@code customer.email()} (the booker's account from signup).
 * {@code app.mail.from} / {@code spring.mail.username} only set who <em>sends</em> the message.
 */
@Service
public class BookingConfirmationMailService {
    private static final Logger log = LoggerFactory.getLogger(BookingConfirmationMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final String fromAddress;
    /** When true, also emails the provider's linked staff account (see seed users). Fake demo emails bounce — leave false unless addresses are real. */
    private final boolean notifyProviderStaff;

    private static final DateTimeFormatter WHEN_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(Locale.getDefault(Locale.Category.FORMAT));

    public BookingConfirmationMailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            ServiceRepository serviceRepository,
            UserRepository userRepository,
            @Value("${app.mail.from:}") String appMailFrom,
            @Value("${spring.mail.username:}") String springMailUsername,
            @Value("${app.mail.notify-provider:false}") boolean notifyProviderStaff
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.notifyProviderStaff = notifyProviderStaff;
        if (appMailFrom != null && !appMailFrom.isBlank()) {
            this.fromAddress = appMailFrom.trim();
        } else if (springMailUsername != null && !springMailUsername.isBlank()) {
            this.fromAddress = springMailUsername.trim();
        } else {
            this.fromAddress = "";
        }
    }

    /**
     * @return notification id when at least the customer email was sent successfully
     */
    public Optional<String> sendBookingConfirmation(
            AppointmentEntity appointment,
            UserEntity customer,
            ProviderEntity provider
    ) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException(
                    "Confirmation email is not available: set spring.mail.host (and SMTP username, password, port) "
                            + "in application.properties or environment variables. See the mail example in application.properties."
            );
        }

        if (fromAddress.isBlank()) {
            log.warn("mail.skip reason=no-from-address set app.mail.from or spring.mail.username");
            return Optional.empty();
        }

        String serviceName = serviceRepository.findById(appointment.serviceId())
                .map(s -> s.name())
                .orElse("Service #" + appointment.serviceId());

        String when = appointment.startTime().format(WHEN_FORMAT) + " – " + appointment.endTime().format(WHEN_FORMAT);
        String ref = String.valueOf(appointment.appointmentId());

        String customerBody = """
                Hello %s,

                Your appointment is confirmed.

                Provider: %s
                Service: %s
                When: %s
                Reference: #%s

                Thank you for booking with us.
                """
                .formatted(
                        customer.name(),
                        provider.displayName(),
                        serviceName,
                        when,
                        ref
                );

        String notificationId = "email-" + ref + "-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            sendMime(mailSender, customer.email(), "Booking confirmed — " + provider.displayName(), customerBody);
            log.info("mail.customer.sent appointmentId={} to={}", appointment.appointmentId(), customer.email());
        } catch (MessagingException ex) {
            log.error("mail.customer.failed appointmentId={}", appointment.appointmentId(), ex);
            String detail = ex.getMessage();
            Throwable c = ex.getCause();
            if (detail == null || detail.isBlank()) {
                detail = ex.getClass().getSimpleName();
            }
            if (c != null && c.getMessage() != null && !c.getMessage().isBlank()) {
                detail = detail + " — " + c.getMessage();
            }
            throw new IllegalStateException("Could not send confirmation email: " + detail, ex);
        }

        if (notifyProviderStaff && provider.userId() != null) {
            userRepository.findById(provider.userId()).ifPresent(providerUser -> {
                String pe = providerUser.email();
                if (pe != null && !pe.isBlank()
                        && !pe.equalsIgnoreCase(customer.email())) {
                    String staffBody = """
                            New booking confirmed.

                            Customer: %s (%s)
                            Service: %s
                            When: %s
                            Appointment reference: #%s
                            """
                            .formatted(
                                    customer.name(),
                                    customer.email(),
                                    serviceName,
                                    when,
                                    ref
                            );
                    try {
                        sendMime(mailSender, pe, "New booking — " + customer.name(), staffBody);
                        log.info("mail.provider.sent appointmentId={} to={}", appointment.appointmentId(), pe);
                    } catch (MessagingException ex) {
                        log.warn("mail.provider.failed appointmentId={} — customer mail already sent", appointment.appointmentId(), ex);
                    }
                }
            });
        }

        return Optional.of(notificationId);
    }

    private void sendMime(JavaMailSender mailSender, String to, String subject, String text) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, false);
        mailSender.send(message);
    }
}
