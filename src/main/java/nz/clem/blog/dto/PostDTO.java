package nz.clem.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Long id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}