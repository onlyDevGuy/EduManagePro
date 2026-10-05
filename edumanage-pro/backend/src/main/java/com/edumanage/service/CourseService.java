package com.edumanage.service;

import com.edumanage.domain.Course;
import com.edumanage.domain.Enrollment;
import com.edumanage.repository.CourseRepository;
import com.edumanage.repository.EnrollmentRepository;
import com.edumanage.repository.StudentRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.web.dto.CommonDtos.PageResponse;
import com.edumanage.web.dto.CourseDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public PageResponse<CourseView> list(int page, int size) {
        String tenantId = TenantContext.requireTenantId();
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("code"));
        return PageResponse.from(
                courseRepository.findAllByTenantId(tenantId, pageable).map(CourseService::toView));
    }

    @Transactional(readOnly = true)
    public CourseView get(String id) { return toView(require(id)); }

    @Transactional
    public CourseView create(CreateCourseRequest req) {
        String tenantId = TenantContext.requireTenantId();
        if (courseRepository.findByTenantIdAndCode(tenantId, req.code().trim().toUpperCase()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course code already exists");
        }
        Course course = Course.builder()
                .code(req.code().trim().toUpperCase())
                .title(req.title().trim())
                .description(req.description())
                .teacherId(req.teacherId())
                .feeAmount(req.feeAmount())
                .currency(req.currency() == null ? "USD" : req.currency().toUpperCase())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .active(true)
                .build();
        return toView(courseRepository.save(course));
    }

    @Transactional
    public CourseView update(String id, UpdateCourseRequest req) {
        Course course = require(id);
        if (req.title() != null)       course.setTitle(req.title().trim());
        if (req.description() != null) course.setDescription(req.description());
        if (req.teacherId() != null)   course.setTeacherId(req.teacherId());
        if (req.feeAmount() != null)   course.setFeeAmount(req.feeAmount());
        if (req.currency() != null)    course.setCurrency(req.currency().toUpperCase());
        if (req.startDate() != null)   course.setStartDate(req.startDate());
        if (req.endDate() != null)     course.setEndDate(req.endDate());
        if (req.active() != null)      course.setActive(req.active());
        return toView(courseRepository.save(course));
    }

    @Transactional
    public void delete(String id) {
        courseRepository.delete(require(id));
    }

    // ---- Enrollment -------------------------------------------------------

    @Transactional
    public EnrollmentView enroll(String courseId, String studentId) {
        String tenantId = TenantContext.requireTenantId();
        Course course = require(courseId);
        if (!course.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course is not active");
        }
        studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (enrollmentRepository.findByTenantIdAndStudentIdAndCourseId(tenantId, studentId, courseId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already enrolled");
        }

        try {
            Enrollment saved = enrollmentRepository.save(Enrollment.builder()
                    .courseId(courseId)
                    .studentId(studentId)
                    .status(Enrollment.Status.ACTIVE)
                    .enrolledOn(LocalDate.now())
                    .build());
            return toView(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already enrolled");
        }
    }

    @Transactional
    public void unenroll(String courseId, String studentId) {
        String tenantId = TenantContext.requireTenantId();
        Enrollment e = enrollmentRepository
                .findByTenantIdAndStudentIdAndCourseId(tenantId, studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        e.setStatus(Enrollment.Status.DROPPED);
        enrollmentRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentView> listEnrollments(String courseId) {
        String tenantId = TenantContext.requireTenantId();
        require(courseId);
        return enrollmentRepository.findAllByTenantIdAndCourseId(tenantId, courseId)
                .stream().map(CourseService::toView).toList();
    }

    Course require(String id) {
        return courseRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    static CourseView toView(Course c) {
        return new CourseView(c.getId(), c.getCode(), c.getTitle(), c.getDescription(),
                c.getTeacherId(), c.getFeeAmount(), c.getCurrency(),
                c.getStartDate(), c.getEndDate(), c.isActive());
    }

    static EnrollmentView toView(Enrollment e) {
        return new EnrollmentView(e.getId(), e.getStudentId(), e.getCourseId(),
                e.getStatus().name(), e.getEnrolledOn());
    }
}