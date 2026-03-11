package nz.clem.blog.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ContentBlockDTO {
    private Long id;
    private String blockType;
    private int sortOrder;
    private Map<String, Object> content;
}
