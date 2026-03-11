package nz.clem.blog.dto;

import lombok.Data;

@Data
public class CreatePageRequest {
    private String title;
    private String slug;
    private String metaDescription;
    private String ogImageUrl;
    private String status;
}
