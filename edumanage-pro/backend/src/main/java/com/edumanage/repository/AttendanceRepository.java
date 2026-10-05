package com.edumanage.repository;

import com.edumanage.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, String> {

    Optional<AttendanceRecord> findByTenantIdAndCourseIdAndStudentIdAndSessionDate(
            String tenantId, String courseId, String studentId, LocalDate sessionDate);

    List<AttendanceRecord> findAllByTenantIdAndCourseIdAndSessionDateBetween(
            String tenantId, String courseId, LocalDate from, LocalDate to);

    List<AttendanceRecord> findAllByTenantIdAndStudentId(String tenantId, String studentId);
}
