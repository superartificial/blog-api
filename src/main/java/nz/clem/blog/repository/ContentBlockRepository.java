package nz.clem.blog.repository;

import nz.clem.blog.entity.ContentBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentBlockRepository extends JpaRepository<ContentBlock, Long> {

    List<ContentBlock> findByPageIdOrderBySortOrderAsc(Long pageId);
}
