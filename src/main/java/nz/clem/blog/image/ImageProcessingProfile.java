package nz.clem.blog.image;

/**
 * Defines resize/quality constraints for different image use-cases.
 *
 * maxWidth     – maximum width in pixels; image is downscaled proportionally if wider
 * maxHeight    – maximum height in pixels (null = proportional to width only)
 * quality      – JPEG quality 0.0–1.0
 * forceFormat  – output format string ("jpeg", "png"); null = keep original format
 *
 * Future profiles can add cropping modes, watermarking, etc. by extending this enum
 * or by introducing a richer config object loaded from application properties.
 */
public enum ImageProcessingProfile {

    /** General content images — hero blocks, image blocks, OG images */
    STANDARD(1920, null, 0.85f, null),

    /** Small JPEG thumbnail always generated alongside the main image */
    THUMBNAIL(300, null, 0.80f, "jpeg"),

    /** Open Graph social-share image (future: center-crop to exact ratio) */
    OG_IMAGE(1200, 630, 0.90f, null),

    /** Full-width hero banners where extra resolution is warranted */
    HERO(2560, null, 0.85f, null);

    public final int maxWidth;
    public final Integer maxHeight;   // null = proportional scaling
    public final float quality;
    public final String forceFormat;  // null = keep original format

    ImageProcessingProfile(int maxWidth, Integer maxHeight, float quality, String forceFormat) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.quality = quality;
        this.forceFormat = forceFormat;
    }
}
