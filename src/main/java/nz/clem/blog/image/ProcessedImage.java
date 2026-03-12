package nz.clem.blog.image;

public record ProcessedImage(byte[] data, String mimeType, long sizeBytes) {}
