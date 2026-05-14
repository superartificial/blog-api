package nz.clem.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostSummaryDTO {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String status;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
