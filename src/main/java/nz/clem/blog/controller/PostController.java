package nz.clem.blog.controller;

import nz.clem.blog.dto.PostDTO;
import nz.clem.blog.dto.PostSummaryDTO;
import nz.clem.blog.entity.Post;
import nz.clem.blog.entity.PostStatus;
import nz.clem.blog.entity.Tag;
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
    private ImageReferenceService imageReferenceService;

    @GetMapping
    public ResponseEntity<List<PostSummaryDTO>> getAllPublishedPosts(@RequestParam(required = false) String tag) {
        List<Post> posts = (tag != null && !tag.isBlank())
                ? postRepository.findByStatusAndTagNameOrderByCreatedAtDesc(PostStatus.PUBLISHED, tag)
                : postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
        return ResponseEntity.ok(posts.stream().map(this::convertToSummaryDTO).collect(Collectors.toList()));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PostSummaryDTO>> getAllPostsAdmin() {
        List<PostSummaryDTO> posts = postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        List<String> tags = tagRepository.findAll()
                .stream()
                .map(Tag::getName)
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PostDTO> getPostBySlug(@PathVariable String slug, Authentication authentication) {
        Optional<Post> post = postRepository.findBySlug(slug);
        if (post.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Post p = post.get();
        if (p.getStatus() != PostStatus.PUBLISHED) {
            boolean isAdmin = authentication != null &&
                    authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                return ResponseEntity.notFound().build();
            }
        }
        return ResponseEntity.ok(convertToDTO(p));
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        return postRepository.findById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPost(@RequestBody PostDTO postDTO) {
        try {
            Post post = new Post();
            post.setTitle(postDTO.getTitle());
            post.setSlug(postDTO.getSlug());
            post.setContent(postDTO.getContent());
            post.setExcerpt(postDTO.getExcerpt());
            post.setHumanIntro(postDTO.getHumanIntro());
            post.setAiNotes(postDTO.getAiNotes());
            post.setStatus(parseStatus(postDTO.getStatus()));
            post.setTags(resolveTags(postDTO.getTags()));

            Post savedPost = postRepository.save(post);
            imageReferenceService.syncForPost(savedPost);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedPost));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A post with slug '" + postDTO.getSlug() + "' already exists"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody PostDTO postDTO) {
        Optional<Post> post = postRepository.findById(id);
        if (post.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Post existingPost = post.get();
            existingPost.setTitle(postDTO.getTitle());
            existingPost.setSlug(postDTO.getSlug());
            existingPost.setContent(postDTO.getContent());
            existingPost.setExcerpt(postDTO.getExcerpt());
            existingPost.setHumanIntro(postDTO.getHumanIntro());
            existingPost.setAiNotes(postDTO.getAiNotes());
            existingPost.setStatus(parseStatus(postDTO.getStatus()));
            existingPost.setTags(resolveTags(postDTO.getTags()));

            Post updatedPost = postRepository.save(existingPost);
            imageReferenceService.syncForPost(updatedPost);
            return ResponseEntity.ok(convertToDTO(updatedPost));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "A post with slug '" + postDTO.getSlug() + "' already exists"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        Optional<Post> post = postRepository.findById(id);
        if (post.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        imageReferenceService.deleteForPost(id);
        postRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return new HashSet<>();
        Set<Tag> tags = new HashSet<>();
        for (String name : tagNames) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;
            Tag tag = tagRepository.findByName(trimmed)
                    .orElseGet(() -> tagRepository.save(new Tag(null, trimmed)));
            tags.add(tag);
        }
        return tags;
    }

    private PostStatus parseStatus(String statusStr) {
        if (statusStr == null) return PostStatus.DRAFT;
        try {
            return PostStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return PostStatus.DRAFT;
        }
    }

    private PostSummaryDTO convertToSummaryDTO(Post post) {
        PostSummaryDTO dto = new PostSummaryDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setSlug(post.getSlug());
        dto.setExcerpt(post.getExcerpt());
        dto.setStatus(post.getStatus().name());
        dto.setTags(post.getTags().stream().map(Tag::getName).sorted().collect(Collectors.toList()));
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
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        return dto;
    }
}
