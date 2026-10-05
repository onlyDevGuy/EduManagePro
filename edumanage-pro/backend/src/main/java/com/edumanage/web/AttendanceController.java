package com.edumanage.web;

import com.edumanage.domain.User;
import com.edumanage.service.AttendanceService;
import com.edumanage.web.dto.AttendanceDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/courses/{courseId}/sessions")
    public BulkResult recordSession(@PathVariable String courseId,
                                    @Valid @RequestBody RecordSessionRequest request,
                                    @AuthenticationPrincipal User actor) {
        return attendanceService.recordSession(courseId, request.sessionDate(), request.entries(), actor);
    }

    @GetMapping("/courses/{courseId}")
    public List<RecordView> listByCourse(
            @PathVariable String courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendanceService.listByCourse(courseId, from, to);
    }

    @GetMapping("/students/{studentId}/summary")
    public List<AttendanceSummary> summaryForStudent(@PathVariable String studentId) {
        return attendanceService.summaryForStudent(studentId);
    }
}
