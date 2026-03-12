package nz.clem.blog.repository;

import nz.clem.blog.entity.ImageReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImageReferenceRepository extends JpaRepository<ImageReference, Long> {

    @Modifying
    @Query("DELETE FROM ImageReference ir WHERE ir.entityType = :entityType AND ir.entityId = :entityId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Modifying
    @Query(nativeQuery = true, value =
            "INSERT INTO image_references (image_id, entity_type, entity_id, field_name) " +
            "VALUES (:imageId, :entityType, :entityId, :fieldName) " +
            "ON CONFLICT (image_id, entity_type, entity_id, field_name) DO NOTHING")
    void insertIfNotExists(@Param("imageId") Long imageId,
                           @Param("entityType") String entityType,
                           @Param("entityId") Long entityId,
                           @Param("fieldName") String fieldName);

    long countByImageId(Long imageId);

    List<ImageReference> findByImageId(Long imageId);
}
