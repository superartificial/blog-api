package nz.clem.blog.controller;

import nz.clem.blog.dto.ImageDTO;
import nz.clem.blog.entity.Image;
import nz.clem.blog.image.ImageProcessingProfile;
import nz.clem.blog.image.ImageProcessingService;
import nz.clem.blog.image.ProcessedImage;
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
    private final ImageProcessingService imageProcessingService;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${r2.public-url}")
    private String publicUrl;

    public ImageController(S3Client s3Client,
                           ImageRepository imageRepository,
                           ImageReferenceRepository imageReferenceRepository,
                           ImageProcessingService imageProcessingService) {
        this.s3Client = s3Client;
        this.imageRepository = imageRepository;
        this.imageReferenceRepository = imageReferenceRepository;
        this.imageProcessingService = imageProcessingService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Only JPEG, PNG, GIF, and WebP images are allowed"));
        }

        byte[] originalBytes = file.getBytes();
        byte[] mainBytes = originalBytes;
        String mainMimeType = contentType;
        String thumbFilename = null;
        String thumbUrl = null;

        if (imageProcessingService.canProcess(contentType)) {
            // Resize main image
            ProcessedImage processed = imageProcessingService.process(originalBytes, contentType, ImageProcessingProfile.STANDARD);
            mainBytes = processed.data();
            mainMimeType = processed.mimeType();

            // Generate JPEG thumbnail
            ProcessedImage thumb = imageProcessingService.process(originalBytes, contentType, ImageProcessingProfile.THUMBNAIL);
            thumbFilename = "thumb_" + UUID.randomUUID() + ".jpg";
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(thumbFilename)
                            .contentType(thumb.mimeType())
                            .build(),
                    RequestBody.fromBytes(thumb.data())
            );
            thumbUrl = publicUrl + "/" + thumbFilename;
        }

        String ext = mainMimeType.substring(mainMimeType.indexOf('/') + 1);
        String filename = UUID.randomUUID() + "." + ext;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(filename)
                        .contentType(mainMimeType)
                        .build(),
                RequestBody.fromBytes(mainBytes)
        );

        String url = publicUrl + "/" + filename;

        Image image = new Image();
        image.setFilename(filename);
        image.setUrl(url);
        image.setMimeType(mainMimeType);
        image.setSizeBytes((long) mainBytes.length);
        image.setThumbnailFilename(thumbFilename);
        image.setThumbnailUrl(thumbUrl);
        Image savedImage = imageRepository.save(image);

        return ResponseEntity.ok(toDTO(savedImage, 0L));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Optional<Image> imageOpt = imageRepository.findById(id);
        if (imageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Image image = imageOpt.get();
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(image.getFilename()).build());

        if (image.getThumbnailFilename() != null) {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(image.getThumbnailFilename()).build());
        }

        imageRepository.delete(image);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ImageDTO>> getAllImages() {
        List<ImageDTO> images = imageRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Image::getUploadedAt).reversed())
                .map(image -> toDTO(image, imageReferenceRepository.countByImageId(image.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(images);
    }

    private ImageDTO toDTO(Image image, long referenceCount) {
        return new ImageDTO(
                image.getId(),
                image.getFilename(),
                image.getUrl(),
                image.getThumbnailUrl(),
                image.getMimeType(),
                image.getSizeBytes(),
                image.getUploadedAt(),
                referenceCount
        );
    }
}
