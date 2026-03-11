package nz.clem.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PageDTO {
    private Long id;
    private String slug;
    private String title;
    private String metaDescription;
    private String ogImageUrl;
    private String status;
    private List<ContentBlockDTO> blocks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
