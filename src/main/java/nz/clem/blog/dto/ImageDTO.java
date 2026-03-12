package nz.clem.blog.dto;

import java.time.LocalDateTime;

public record ImageDTO(Long id, String filename, String url, String thumbnailUrl, String mimeType, Long sizeBytes, LocalDateTime uploadedAt, long referenceCount) {}
