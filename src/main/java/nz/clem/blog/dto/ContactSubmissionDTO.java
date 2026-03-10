package nz.clem.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactSubmissionDTO {
    private Long id;
    private String name;
    private String email;
    private String message;
    private String ipAddress;
    private LocalDateTime submittedAt;
    private Boolean read;
}
