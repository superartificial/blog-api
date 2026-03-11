package nz.clem.blog.repository;

import nz.clem.blog.entity.Page;
import nz.clem.blog.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Long> {

    Optional<Page> findBySlugAndStatus(String slug, PostStatus status);

    List<Page> findByStatusOrderByCreatedAtDesc(PostStatus status);

    List<Page> findAllByOrderByCreatedAtDesc();
}
