package nz.clem.blog.dto;

import java.time.LocalDateTime;

public record ImageDTO(Long id, String filename, String url, String mimeType, Long sizeBytes, LocalDateTime uploadedAt, long referenceCount) {}
