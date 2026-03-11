package nz.clem.blog.repository;

import nz.clem.blog.entity.Post;
import nz.clem.blog.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    List<Post> findByStatusOrderByCreatedAtDesc(PostStatus status);
    List<Post> findAllByOrderByCreatedAtDesc();
}
