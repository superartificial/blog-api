package nz.clem.blog.repository;

import nz.clem.blog.entity.Post;
import nz.clem.blog.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    List<Post> findByStatusOrderByCreatedAtDesc(PostStatus status);
    List<Post> findAllByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT p FROM Post p JOIN p.tags t WHERE p.status = :status AND t.name = :tagName ORDER BY p.createdAt DESC")
    List<Post> findByStatusAndTagNameOrderByCreatedAtDesc(@Param("status") PostStatus status, @Param("tagName") String tagName);

    @Query("SELECT p FROM Post p WHERE p.status = :status AND (p.category.id = :categoryId OR p.category.parentId = :categoryId) ORDER BY p.createdAt DESC")
    List<Post> findByStatusAndCategoryOrderByCreatedAtDesc(@Param("status") PostStatus status, @Param("categoryId") Long categoryId);
}
