package nz.clem.blog.controller;

import nz.clem.blog.entity.Post;
import nz.clem.blog.entity.PostStatus;
import nz.clem.blog.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostController controller;

    // MockMvc wraps the controller in a lightweight HTTP layer —
    // no Spring context, no security, no database. Just routing + JSON.
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllPublishedPosts_returns200WithList() throws Exception {
        // given
        Post post = new Post();
        post.setId(1L);
        post.setTitle("Hello World");
        post.setSlug("hello-world");
        post.setContent("Some content");
        post.setStatus(PostStatus.PUBLISHED);

        when(postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED)).thenReturn(List.of(post));

        // when / then
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Hello World"))
                .andExpect(jsonPath("$[0].slug").value("hello-world"));
    }

    @Test
    void getAllPublishedPosts_returnsEmptyList_whenNoPosts() throws Exception {
        when(postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED)).thenReturn(List.of());

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPostBySlug_returns200_whenPostIsPublished() throws Exception {
        // given
        Post post = new Post();
        post.setId(1L);
        post.setTitle("Hello World");
        post.setSlug("hello-world");
        post.setContent("Some content");
        post.setStatus(PostStatus.PUBLISHED);

        when(postRepository.findBySlug("hello-world")).thenReturn(Optional.of(post));

        // when / then
        mockMvc.perform(get("/api/posts/hello-world"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hello World"))
                .andExpect(jsonPath("$.slug").value("hello-world"));
    }

    @Test
    void getPostBySlug_returns404_whenSlugNotFound() throws Exception {
        when(postRepository.findBySlug("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/posts/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPostBySlug_returns404_whenPostIsUnpublished() throws Exception {
        // given — slug exists, but the post is a draft
        Post draft = new Post();
        draft.setId(2L);
        draft.setTitle("Draft Post");
        draft.setSlug("draft-post");
        draft.setContent("Work in progress");
        draft.setStatus(PostStatus.DRAFT);

        when(postRepository.findBySlug("draft-post")).thenReturn(Optional.of(draft));

        // when / then — non-published posts should be invisible to public readers
        mockMvc.perform(get("/api/posts/draft-post"))
                .andExpect(status().isNotFound());
    }
}
