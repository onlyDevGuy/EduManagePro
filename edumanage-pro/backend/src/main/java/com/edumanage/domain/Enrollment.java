package com.edumanage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "enrollments",
       uniqueConstraints = @UniqueConstraint(name = "uk_enrollments",
                                             columnNames = {"tenant_id", "student_id", "course_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Enrollment extends TenantAwareEntity {

    public enum Status { ACTIVE, COMPLETED, DROPPED }

    @Column(name = "student_id", nullable = false, length = 36)
    private String studentId;

    @Column(name = "course_id", nullable = false, length = 36)
    private String courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "enrolled_on", nullable = false)
    @Builder.Default
    private LocalDate enrolledOn = LocalDate.now();
}