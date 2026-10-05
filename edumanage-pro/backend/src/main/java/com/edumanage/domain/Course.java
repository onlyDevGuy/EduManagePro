package com.edumanage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "courses",
       uniqueConstraints = @UniqueConstraint(name = "uk_courses_tenant_code",
                                             columnNames = {"tenant_id", "code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course extends TenantAwareEntity {

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "teacher_id", length = 36)
    private String teacherId;

    @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}