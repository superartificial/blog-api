package nz.clem.blog.service;

import nz.clem.blog.entity.ContentBlock;
import nz.clem.blog.entity.Page;
import nz.clem.blog.entity.Post;
import nz.clem.blog.repository.ImageReferenceRepository;
import nz.clem.blog.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImageReferenceService {

    private static final Pattern MARKDOWN_IMAGE_PATTERN =
            Pattern.compile("!\\[[^]]*]\\(([^)]+)\\)");

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageReferenceRepository imageReferenceRepository;

    @Transactional
    public void syncForPost(Post post) {
        imageReferenceRepository.deleteByEntityTypeAndEntityId("POST", post.getId());

        Set<String> urls = new HashSet<>();
        urls.addAll(extractMarkdownImageUrls(post.getContent()));
        urls.addAll(extractMarkdownImageUrls(post.getHumanIntro()));
        urls.addAll(extractMarkdownImageUrls(post.getAiNotes()));

        for (String url : urls) {
            imageRepository.findByUrl(url).ifPresent(image ->
                    imageReferenceRepository.insertIfNotExists(image.getId(), "POST", post.getId(), "content")
            );
        }
    }

    @Transactional
    public void syncForBlock(ContentBlock block) {
        imageReferenceRepository.deleteByEntityTypeAndEntityId("CONTENT_BLOCK", block.getId());

        Set<String> urls = new HashSet<>();
        String fieldName = block.getBlockType().name().toLowerCase();

        switch (block.getBlockType()) {
            case IMAGE -> {
                Object urlObj = block.getContent().get("url");
                if (urlObj instanceof String s && !s.isBlank()) {
                    urls.add(s);
                }
            }
            case HERO -> {
                Object urlObj = block.getContent().get("bgImageUrl");
                if (urlObj instanceof String s && !s.isBlank()) {
                    urls.add(s);
                }
            }
            case RICH_TEXT -> {
                Object bodyObj = block.getContent().get("body");
                if (bodyObj instanceof String s) {
                    urls.addAll(extractMarkdownImageUrls(s));
                }
            }
            default -> {
                // No image fields for other block types
            }
        }

        for (String url : urls) {
            imageRepository.findByUrl(url).ifPresent(image ->
                    imageReferenceRepository.insertIfNotExists(image.getId(), "CONTENT_BLOCK", block.getId(), fieldName)
            );
        }
    }

    @Transactional
    public void syncForPage(Page page) {
        imageReferenceRepository.deleteByEntityTypeAndEntityId("PAGE", page.getId());

        String ogImageUrl = page.getOgImageUrl();
        if (ogImageUrl != null && !ogImageUrl.isBlank()) {
            imageRepository.findByUrl(ogImageUrl).ifPresent(image ->
                    imageReferenceRepository.insertIfNotExists(image.getId(), "PAGE", page.getId(), "ogImageUrl")
            );
        }
    }

    @Transactional
    public void deleteForPost(Long postId) {
        imageReferenceRepository.deleteByEntityTypeAndEntityId("POST", postId);
    }

    @Transactional
    public void deleteForBlock(Long blockId) {
        imageReferenceRepository.deleteByEntityTypeAndEntityId("CONTENT_BLOCK", blockId);
    }

    @Transactional
    public void deleteForPage(Long pageId) {
        imageReferenceRepository.deleteByEntityTypeAndEntityId("PAGE", pageId);
    }

    private Set<String> extractMarkdownImageUrls(String text) {
        Set<String> urls = new HashSet<>();
        if (text == null || text.isBlank()) {
            return urls;
        }
        Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }
}
