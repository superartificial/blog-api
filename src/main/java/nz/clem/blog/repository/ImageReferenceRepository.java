package nz.clem.blog.repository;

import nz.clem.blog.entity.ImageReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImageReferenceRepository extends JpaRepository<ImageReference, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ImageReference ir WHERE ir.entityType = :entityType AND ir.entityId = :entityId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    long countByImageId(Long imageId);

    List<ImageReference> findByImageId(Long imageId);
}
