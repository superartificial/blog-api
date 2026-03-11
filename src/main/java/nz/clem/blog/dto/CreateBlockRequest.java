package nz.clem.blog.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CreateBlockRequest {
    private String blockType;
    private Integer sortOrder;
    private Map<String, Object> content;
}
