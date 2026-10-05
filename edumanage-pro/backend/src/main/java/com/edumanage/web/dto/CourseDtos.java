package com.edumanage.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CourseDtos {

    private CourseDtos() {}

    public record CreateCourseRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            String teacherId,
            @NotNull @DecimalMin("0.00") BigDecimal feeAmount,
            @Size(min = 3, max = 3) String currency,
            LocalDate startDate,
            LocalDate endDate) {}

    public record UpdateCourseRequest(
            @Size(max = 200) String title,
            @Size(max = 2000) String description,
            String teacherId,
            @DecimalMin("0.00") BigDecimal feeAmount,
            @Size(min = 3, max = 3) String currency,
            LocalDate startDate,
            LocalDate endDate,
            Boolean active) {}

    public record CourseView(String id, String code, String title, String description,
                             String teacherId, BigDecimal feeAmount, String currency,
                             LocalDate startDate, LocalDate endDate, boolean active) {}

    public record EnrollRequest(@NotBlank String studentId) {}

    public record EnrollmentView(String id, String studentId, String courseId,
                                 String status, LocalDate enrolledOn) {}
}