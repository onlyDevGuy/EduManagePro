package com.edumanage.service;

import com.edumanage.domain.AttendanceRecord;
import com.edumanage.domain.User;
import com.edumanage.repository.AttendanceRepository;
import com.edumanage.repository.EnrollmentRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.web.dto.AttendanceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;

    /**
     * Idempotent bulk upsert for a single class session — the common UX is a teacher
     * marking the whole roster at once, then correcting entries later.
     */
    @Transactional
    public BulkResult recordSession(String courseId, LocalDate sessionDate, List<Entry> entries, User actor) {
        String tenantId = TenantContext.requireTenantId();
        courseService.require(courseId);

        List<String> unknown = entries.stream()
                .map(Entry::studentId)
                .filter(sid -> enrollmentRepository
                        .findByTenantIdAndStudentIdAndCourseId(tenantId, sid, courseId).isEmpty())
                .toList();
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Not enrolled in this course: " + String.join(", ", unknown));
        }

        int created = 0, updated = 0;
        for (Entry entry : entries) {
            var existing = attendanceRepository
                    .findByTenantIdAndCourseIdAndStudentIdAndSessionDate(
                            tenantId, courseId, entry.studentId(), sessionDate);

            if (existing.isPresent()) {
                AttendanceRecord rec = existing.get();
                rec.setStatus(entry.status());
                rec.setNote(entry.note());
                rec.setRecordedBy(actor.getId());
                attendanceRepository.save(rec);
                updated++;
            } else {
                attendanceRepository.save(AttendanceRecord.builder()
                        .courseId(courseId)
                        .studentId(entry.studentId())
                        .sessionDate(sessionDate)
                        .status(entry.status())
                        .note(entry.note())
                        .recordedBy(actor.getId())
                        .build());
                created++;
            }
        }
        return new BulkResult(sessionDate, created, updated, entries.size());
    }

    @Transactional(readOnly = true)
    public List<RecordView> listByCourse(String courseId, LocalDate from, LocalDate to) {
        String tenantId = TenantContext.requireTenantId();
        courseService.require(courseId);
        return attendanceRepository
                .findAllByTenantIdAndCourseIdAndSessionDateBetween(tenantId, courseId, from, to)
                .stream()
                .sorted(Comparator.comparing(AttendanceRecord::getSessionDate).reversed())
                .map(AttendanceService::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummary> summaryForStudent(String studentId) {
        String tenantId = TenantContext.requireTenantId();
        Map<String, EnumMap<AttendanceRecord.Status, Integer>> byCourse = new LinkedHashMap<>();

        for (AttendanceRecord r : attendanceRepository.findAllByTenantIdAndStudentId(tenantId, studentId)) {
            byCourse.computeIfAbsent(r.getCourseId(), k -> new EnumMap<>(AttendanceRecord.Status.class))
                    .merge(r.getStatus(), 1, Integer::sum);
        }

        return byCourse.entrySet().stream().map(e -> {
            var counts = e.getValue();
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            int present = counts.getOrDefault(AttendanceRecord.Status.PRESENT, 0);
            int late    = counts.getOrDefault(AttendanceRecord.Status.LATE, 0);
            double rate = total == 0 ? 0.0 : Math.round((present + late) * 1000.0 / total) / 10.0;
            return new AttendanceSummary(e.getKey(), total,
                    counts.getOrDefault(AttendanceRecord.Status.PRESENT, 0),
                    counts.getOrDefault(AttendanceRecord.Status.ABSENT, 0),
                    counts.getOrDefault(AttendanceRecord.Status.LATE, 0),
                    counts.getOrDefault(AttendanceRecord.Status.EXCUSED, 0),
                    rate);
        }).toList();
    }

    static RecordView toView(AttendanceRecord r) {
        return new RecordView(r.getId(), r.getCourseId(), r.getStudentId(),
                r.getSessionDate(), r.getStatus().name(), r.getNote());
    }
}