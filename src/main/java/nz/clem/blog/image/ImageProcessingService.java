package nz.clem.blog.image;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

@Service
public class ImageProcessingService {

    /** MIME types we can resize. WebP and GIF are passed through unchanged. */
    private static final Set<String> PROCESSABLE = Set.of("image/jpeg", "image/png");

    public boolean canProcess(String mimeType) {
        return mimeType != null && PROCESSABLE.contains(mimeType);
    }

    /**
     * Resize and/or re-encode {@code input} according to {@code profile}.
     * <p>
     * Images that already fit within the profile's bounds are returned as-is
     * (no upscaling). Format conversion is still applied when forceFormat is set.
     */
    public ProcessedImage process(byte[] input, String mimeType, ImageProcessingProfile profile) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(input));
        if (original == null) {
            // Unreadable image — return raw bytes untouched
            return new ProcessedImage(input, mimeType, input.length);
        }

        int origWidth = original.getWidth();
        int origHeight = original.getHeight();

        boolean needsResize = origWidth > profile.maxWidth
                || (profile.maxHeight != null && origHeight > profile.maxHeight);
        boolean needsFormatChange = profile.forceFormat != null
                && !mimeTypeMatchesFormat(mimeType, profile.forceFormat);

        if (!needsResize && !needsFormatChange) {
            return new ProcessedImage(input, mimeType, input.length);
        }

        String outputFormat = profile.forceFormat != null
                ? profile.forceFormat
                : (mimeType.equals("image/png") ? "png" : "jpeg");
        String outputMimeType = "image/" + outputFormat;

        var builder = Thumbnails.of(original).outputQuality(profile.quality);

        if (needsResize) {
            if (profile.maxHeight != null) {
                // Fit within a bounding box, preserving aspect ratio
                builder.size(profile.maxWidth, profile.maxHeight).keepAspectRatio(true);
            } else {
                // Downscale width only; origWidth > maxWidth is guaranteed here
                builder.width(profile.maxWidth);
            }
        } else {
            // Re-encode without resizing (format conversion only)
            builder.size(origWidth, origHeight);
        }

        var out = new ByteArrayOutputStream();
        builder.outputFormat(outputFormat).toOutputStream(out);
        byte[] data = out.toByteArray();
        return new ProcessedImage(data, outputMimeType, data.length);
    }

    private boolean mimeTypeMatchesFormat(String mimeType, String format) {
        return mimeType.equals("image/" + format)
                || (format.equals("jpeg") && mimeType.equals("image/jpg"));
    }
}
