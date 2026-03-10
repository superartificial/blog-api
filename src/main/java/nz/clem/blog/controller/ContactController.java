package nz.clem.blog.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import nz.clem.blog.dto.ContactRequest;
import nz.clem.blog.dto.ContactSubmissionDTO;
import nz.clem.blog.entity.ContactSubmission;
import nz.clem.blog.exception.TooManyRequestsException;
import nz.clem.blog.repository.ContactRepository;
import nz.clem.blog.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final ContactRepository contactRepository;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody ContactRequest request, HttpServletRequest httpRequest) {
        String ip = extractIp(httpRequest);
        try {
            contactService.submit(request, ip);
            return ResponseEntity.ok(Map.of("message", "Thank you for your message!"));
        } catch (TooManyRequestsException e) {
            return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<ContactSubmissionDTO> list() {
        return contactRepository.findAllByOrderByReadAscSubmittedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        return contactRepository.findById(id).map(sub -> {
            sub.setRead(true);
            contactRepository.save(sub);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private ContactSubmissionDTO toDTO(ContactSubmission s) {
        return new ContactSubmissionDTO(
                s.getId(), s.getName(), s.getEmail(), s.getMessage(),
                s.getIpAddress(), s.getSubmittedAt(), s.getRead()
        );
    }

    private String extractIp(HttpServletRequest request) {
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
