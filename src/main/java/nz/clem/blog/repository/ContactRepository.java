package nz.clem.blog.repository;

import nz.clem.blog.entity.ContactSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<ContactSubmission, Long> {
    List<ContactSubmission> findAllByOrderByReadAscSubmittedAtDesc();
}
