package com.edumanage.web.dto;

import com.edumanage.domain.AttendanceRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {

    private AttendanceDtos() {}

    public record Entry(
            @NotNull String studentId,
            @NotNull AttendanceRecord.Status status,
            @Size(max = 300) String note) {}

    public record RecordSessionRequest(
            @NotNull LocalDate sessionDate,
            @NotEmpty @Valid List<Entry> entries) {}

    public record BulkResult(LocalDate sessionDate, int created, int updated, int total) {}

    public record RecordView(String id, String courseId, String studentId,
                             LocalDate sessionDate, String status, String note) {}

    public record AttendanceSummary(String courseId, int totalSessions, int present,
                                    int absent, int late, int excused, double attendanceRate) {}
}