package nz.clem.blog.controller;

import nz.clem.blog.dto.PostDTO;
import nz.clem.blog.dto.PostSummaryDTO;
import nz.clem.blog.entity.Category;
import nz.clem.blog.entity.Post;
import nz.clem.blog.entity.PostStatus;
import nz.clem.blog.entity.Tag;
import nz.clem.blog.repository.CategoryRepository;
import nz.clem.blog.repository.PostRepository;
import nz.clem.blog.repository.TagRepository;
import nz.clem.blog.service.ImageReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ImageReferenceService imageReferenceService;

    @GetMapping
    public ResponseEntity<List<PostSummaryDTO>> getAllPublishedPosts(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String category) {
        List<Post> posts;
        if (tag != null && !tag.isBlank()) {
            posts = postRepository.findByStatusAndTagNameOrderByCreatedAtDesc(PostStatus.PUBLISHED, tag);
        } else if (category != null && !category.isBlank()) {
            Optional<Category> cat = categoryRepository.findBySlug(category);
            posts = cat.isPresent()
                    ? postRepository.findByStatusAndCategoryOrderByCreatedAtDesc(PostStatus.PUBLISHED, cat.get().getId())
                    : List.of();
        } else {
            posts = postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
        }
        return ResponseEntity.ok(posts.stream().map(this::convertToSummaryDTO).collect(Collectors.toList()));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PostSummaryDTO>> getAllPostsAdmin() {
        return ResponseEntity.ok(postRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::convertToSummaryDTO).collect(Collectors.toList()));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        return ResponseEntity.ok(tagRepository.findAll()
                .stream().map(Tag::getName).sorted().collect(Collectors.toList()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PostDTO> getPostBySlug(@PathVariable String slug, Authentication authentication) {
        Optional<Post> post = postRepository.findBySlug(slug);
        if (post.isEmpty()) return ResponseEntity.notFound().build();
        Post p = post.get();
        if (p.getStatus() != PostStatus.PUBLISHED) {
            boolean isAdmin = authentication != null &&
                    authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(convertToDTO(p));
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        return postRepository.findById(id)
                .map(this::convertToDTO).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPost(@RequestBody PostDTO postDTO) {
        try {
            Post post = new Post();
            applyDTO(post, postDTO);
            Post saved = postRepository.save(post);
            imageReferenceService.syncForPost(saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A post with slug '" + postDTO.getSlug() + "' already exists"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody PostDTO postDTO) {
        Optional<Post> post = postRepository.findById(id);
        if (post.isEmpty()) return ResponseEntity.notFound().build();
        try {
            Post existing = post.get();
            applyDTO(existing, postDTO);
            Post updated = postRepository.save(existing);
            imageReferenceService.syncForPost(updated);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A post with slug '" + postDTO.getSlug() + "' already exists"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        if (postRepository.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        imageReferenceService.deleteForPost(id);
        postRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applyDTO(Post post, PostDTO dto) {
        post.setTitle(dto.getTitle());
        post.setSlug(dto.getSlug());
        post.setContent(dto.getContent());
        post.setExcerpt(dto.getExcerpt());
        post.setHumanIntro(dto.getHumanIntro());
        post.setAiNotes(dto.getAiNotes());
        post.setStatus(parseStatus(dto.getStatus()));
        post.setTags(resolveTags(dto.getTags()));
        post.setCategory(dto.getCategoryId() != null
                ? categoryRepository.findById(dto.getCategoryId()).orElse(null)
                : null);
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return new HashSet<>();
        Set<Tag> tags = new HashSet<>();
        for (String name : tagNames) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;
            tags.add(tagRepository.findByName(trimmed).orElseGet(() -> tagRepository.save(new Tag(null, trimmed))));
        }
        return tags;
    }

    private PostStatus parseStatus(String statusStr) {
        if (statusStr == null) return PostStatus.DRAFT;
        try { return PostStatus.valueOf(statusStr); } catch (IllegalArgumentException e) { return PostStatus.DRAFT; }
    }

    private PostSummaryDTO convertToSummaryDTO(Post post) {
        PostSummaryDTO dto = new PostSummaryDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setSlug(post.getSlug());
        dto.setExcerpt(post.getExcerpt());
        dto.setStatus(post.getStatus().name());
        dto.setTags(post.getTags().stream().map(Tag::getName).sorted().collect(Collectors.toList()));
        if (post.getCategory() != null) {
            dto.setCategoryId(post.getCategory().getId());
            dto.setCategoryName(post.getCategory().getName());
            dto.setCategorySlug(post.getCategory().getSlug());
        }
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        return dto;
    }

    private PostDTO convertToDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setSlug(post.getSlug());
        dto.setContent(post.getContent());
        dto.setExcerpt(post.getExcerpt());
        dto.setHumanIntro(post.getHumanIntro());
        dto.setAiNotes(post.getAiNotes());
        dto.setStatus(post.getStatus().name());
        dto.setTags(post.getTags().stream().map(Tag::getName).sorted().collect(Collectors.toList()));
        if (post.getCategory() != null) {
            dto.setCategoryId(post.getCategory().getId());
            dto.setCategoryName(post.getCategory().getName());
            dto.setCategorySlug(post.getCategory().getSlug());
        }
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        return dto;
    }
}
