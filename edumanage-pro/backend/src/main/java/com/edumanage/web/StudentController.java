package com.edumanage.web;

import com.edumanage.service.StudentService;
import com.edumanage.web.dto.CommonDtos.PageResponse;
import com.edumanage.web.dto.StudentDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public PageResponse<StudentView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        return studentService.list(page, size, q);
    }

    @GetMapping("/{id}")
    public StudentView get(@PathVariable String id) {
        return studentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public StudentView create(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public StudentView update(@PathVariable String id, @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable String id) {
        studentService.delete(id);
    }
}
