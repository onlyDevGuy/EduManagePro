package com.edumanage.web;

import com.edumanage.service.CourseService;
import com.edumanage.web.dto.CommonDtos.PageResponse;
import com.edumanage.web.dto.CourseDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public PageResponse<CourseView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return courseService.list(page, size);
    }

    @GetMapping("/{id}")
    public CourseView get(@PathVariable String id) {
        return courseService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public CourseView create(@Valid @RequestBody CreateCourseRequest request) {
        return courseService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public CourseView update(@PathVariable String id, @Valid @RequestBody UpdateCourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable String id) {
        courseService.delete(id);
    }

    // ---- Enrollments ------------------------------------------------------

    @GetMapping("/{id}/enrollments")
    public List<EnrollmentView> listEnrollments(@PathVariable String id) {
        return courseService.listEnrollments(id);
    }

    @PostMapping("/{id}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public EnrollmentView enroll(@PathVariable String id, @Valid @RequestBody EnrollRequest request) {
        return courseService.enroll(id, request.studentId());
    }

    @DeleteMapping("/{id}/enrollments/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public void unenroll(@PathVariable String id, @PathVariable String studentId) {
        courseService.unenroll(id, studentId);
    }
}
