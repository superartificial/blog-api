package nz.clem.blog.controller;

import nz.clem.blog.dto.ImageDTO;
import nz.clem.blog.entity.Image;
import nz.clem.blog.repository.ImageReferenceRepository;
import nz.clem.blog.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final S3Client s3Client;
    private final ImageRepository imageRepository;
    private final ImageReferenceRepository imageReferenceRepository;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${r2.public-url}")
    private String publicUrl;

    public ImageController(S3Client s3Client,
                           ImageRepository imageRepository,
                           ImageReferenceRepository imageReferenceRepository) {
        this.s3Client = s3Client;
        this.imageRepository = imageRepository;
        this.imageReferenceRepository = imageReferenceRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Only JPEG, PNG, GIF, and WebP images are allowed"));
        }

        String ext = contentType.substring(contentType.indexOf('/') + 1);
        String filename = UUID.randomUUID() + "." + ext;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(filename)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        String url = publicUrl + "/" + filename;

        Image image = new Image();
        image.setFilename(filename);
        image.setUrl(url);
        image.setMimeType(contentType);
        image.setSizeBytes(file.getSize());
        Image savedImage = imageRepository.save(image);

        ImageDTO dto = new ImageDTO(
                savedImage.getId(),
                savedImage.getFilename(),
                savedImage.getUrl(),
                savedImage.getMimeType(),
                savedImage.getSizeBytes(),
                savedImage.getUploadedAt(),
                0L
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<Image> imageOpt = imageRepository.findById(id);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Image image = imageOpt.get();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(image.getFilename())
                .build());

        imageRepository.delete(image);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ImageDTO>> getAllImages() {
        List<ImageDTO> images = imageRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Image::getUploadedAt).reversed())
                .map(image -> new ImageDTO(
                        image.getId(),
                        image.getFilename(),
                        image.getUrl(),
                        image.getMimeType(),
                        image.getSizeBytes(),
                        image.getUploadedAt(),
                        imageReferenceRepository.countByImageId(image.getId())
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(images);
    }
}
