package com.edumanage.repository;

import com.edumanage.domain.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, String> {
    Page<Course> findAllByTenantId(String tenantId, Pageable pageable);
    Optional<Course> findByIdAndTenantId(String id, String tenantId);
    Optional<Course> findByTenantIdAndCode(String tenantId, String code);
}
