package nz.clem.blog.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateBlockRequest {
    private String blockType;
    private Map<String, Object> content;
}
