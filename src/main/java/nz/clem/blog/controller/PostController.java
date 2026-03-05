package nz.clem.blog.controller;

import nz.clem.blog.dto.PostDTO;
import nz.clem.blog.entity.Post;
import nz.clem.blog.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPublishedPosts() {
        List<PostDTO> posts = postRepository.findByPublishedTrue()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PostDTO> getPostBySlug(@PathVariable String slug) {
        Optional<Post> post = postRepository.findBySlug(slug);
        if (post.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!post.get().getPublished()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(convertToDTO(post.get()));
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
            post.setPublished(postDTO.getPublished() != null && postDTO.getPublished());

            Post savedPost = postRepository.save(post);
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
            existingPost.setPublished(postDTO.getPublished() != null && postDTO.getPublished());

            Post updatedPost = postRepository.save(existingPost);
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
        postRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PostDTO convertToDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setSlug(post.getSlug());
        dto.setContent(post.getContent());
        dto.setExcerpt(post.getExcerpt());
        dto.setPublished(post.getPublished());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        return dto;
    }
}
