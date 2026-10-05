package com.edumanage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "attendance_records",
       uniqueConstraints = @UniqueConstraint(name = "uk_attendance",
                                             columnNames = {"tenant_id", "course_id", "student_id", "session_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceRecord extends TenantAwareEntity {

    public enum Status { PRESENT, ABSENT, LATE, EXCUSED }

    @Column(name = "course_id", nullable = false, length = 36)
    private String courseId;

    @Column(name = "student_id", nullable = false, length = 36)
    private String studentId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(length = 300)
    private String note;

    @Column(name = "recorded_by", length = 36)
    private String recordedBy;
}