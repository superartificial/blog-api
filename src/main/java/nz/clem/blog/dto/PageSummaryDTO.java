package nz.clem.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PageSummaryDTO {
    private Long id;
    private String slug;
    private String title;
    private String metaDescription;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
