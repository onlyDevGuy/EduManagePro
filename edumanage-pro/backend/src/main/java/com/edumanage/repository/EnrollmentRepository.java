package com.edumanage.repository;

import com.edumanage.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {
    Optional<Enrollment> findByTenantIdAndStudentIdAndCourseId(String tenantId, String studentId, String courseId);
    List<Enrollment> findAllByTenantIdAndCourseId(String tenantId, String courseId);
}
