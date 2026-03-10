package nz.clem.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nz.clem.blog.dto.ContactRequest;
import nz.clem.blog.entity.ContactSubmission;
import nz.clem.blog.exception.TooManyRequestsException;
import nz.clem.blog.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${contact.email.to:}")
    private String emailTo;

    @Value("${contact.email.from:noreply@clem.nz}")
    private String emailFrom;

    private static final int MAX_PER_HOUR = 3;
    private final Map<String, Deque<Instant>> rateLimiter = new ConcurrentHashMap<>();

    public void submit(ContactRequest request, String ipAddress) {
        // Honeypot — bots tend to fill hidden fields
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            log.debug("Honeypot triggered from IP: {}", ipAddress);
            return;
        }

        validate(request);

        if (isRateLimited(ipAddress)) {
            throw new TooManyRequestsException("Too many submissions. Please try again later.");
        }

        ContactSubmission submission = new ContactSubmission();
        submission.setName(request.getName().trim());
        submission.setEmail(request.getEmail().trim().toLowerCase());
        submission.setMessage(request.getMessage().trim());
        submission.setIpAddress(ipAddress);
        contactRepository.save(submission);

        recordSubmission(ipAddress);

        CompletableFuture.runAsync(() -> {
            try {
                sendEmail(submission);
            } catch (Exception e) {
                log.warn("Failed to send contact notification email: {}", e.getMessage());
            }
        });
    }

    private void validate(ContactRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("Name must be 100 characters or fewer.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (request.getEmail().length() > 200) {
            throw new IllegalArgumentException("Email must be 200 characters or fewer.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message is required.");
        }
        if (request.getMessage().length() > 5000) {
            throw new IllegalArgumentException("Message must be 5,000 characters or fewer.");
        }
    }

    private boolean isRateLimited(String ip) {
        Deque<Instant> timestamps = rateLimiter.getOrDefault(ip, new ArrayDeque<>());
        pruneOld(timestamps);
        return timestamps.size() >= MAX_PER_HOUR;
    }

    private void recordSubmission(String ip) {
        rateLimiter.compute(ip, (k, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            pruneOld(deque);
            deque.addLast(Instant.now());
            return deque;
        });
    }

    private void pruneOld(Deque<Instant> deque) {
        Instant cutoff = Instant.now().minusSeconds(3600);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
        }
    }

    private void sendEmail(ContactSubmission submission) {
        if (mailSender == null || emailTo.isBlank()) {
            log.debug("Email not configured — skipping notification.");
            return;
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(emailFrom);
        mail.setTo(emailTo);
        mail.setSubject("New contact from " + submission.getName());
        mail.setText(
                "Name: " + submission.getName() + "\n" +
                "Email: " + submission.getEmail() + "\n\n" +
                "Message:\n" + submission.getMessage() + "\n\n" +
                "---\nIP: " + submission.getIpAddress()
        );
        mailSender.send(mail);
    }
}
