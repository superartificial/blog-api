package nz.clem.blog.repository;

import nz.clem.blog.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByUrl(String url);

    Optional<Image> findByFilename(String filename);
}
