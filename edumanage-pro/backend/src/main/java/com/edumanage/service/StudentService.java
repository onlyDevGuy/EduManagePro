package com.edumanage.service;

import com.edumanage.domain.Student;
import com.edumanage.repository.StudentRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.web.dto.CommonDtos.PageResponse;
import com.edumanage.web.dto.StudentDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public PageResponse<StudentView> list(int page, int size, String q) {
        String tenantId = TenantContext.requireTenantId();
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("lastName", "firstName"));

        Page<Student> result = (q == null || q.isBlank())
                ? studentRepository.findAllByTenantId(tenantId, pageable)
                : studentRepository.search(tenantId, q.trim(), pageable);

        return PageResponse.from(result.map(StudentService::toView));
    }

    @Transactional(readOnly = true)
    public StudentView get(String id) {
        return toView(require(id));
    }

    @Transactional
    public StudentView create(CreateStudentRequest req) {
        String normalizedEmail = normalize(req.email());
        if (normalizedEmail != null && studentRepository.existsByEmailAndTenantId(normalizedEmail, TenantContext.requireTenantId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student with this email already exists");
        }
        
        Student student = Student.builder()
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .email(normalizedEmail)
                .dateOfBirth(req.dateOfBirth())
                .guardianName(req.guardianName())
                .guardianEmail(normalize(req.guardianEmail()))
                .guardianPhone(req.guardianPhone())
                .status(req.status() == null ? Student.Status.ACTIVE : req.status())
                .build();
        return toView(studentRepository.save(student));
    }

    @Transactional
    public StudentView update(String id, UpdateStudentRequest req) {
        Student student = require(id);
        if (req.firstName() != null) student.setFirstName(req.firstName().trim());
        if (req.lastName()  != null) student.setLastName(req.lastName().trim());
        
        if (req.email() != null) {
            String normalizedEmail = normalize(req.email());
            if (normalizedEmail != null && !normalizedEmail.equals(student.getEmail()) 
                && studentRepository.existsByEmailAndTenantId(normalizedEmail, TenantContext.requireTenantId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Student with this email already exists");
            }
            student.setEmail(normalizedEmail);
        }
        if (req.dateOfBirth() != null) student.setDateOfBirth(req.dateOfBirth());
        if (req.guardianName()  != null) student.setGuardianName(req.guardianName());
        if (req.guardianEmail() != null) student.setGuardianEmail(normalize(req.guardianEmail()));
        if (req.guardianPhone() != null) student.setGuardianPhone(req.guardianPhone());
        if (req.status() != null) student.setStatus(req.status());
        return toView(studentRepository.save(student));
    }

    @Transactional
    public void delete(String id) {
        Student student = require(id);
        studentRepository.delete(student);
    }

    Student require(String id) {
        return studentRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    private static String normalize(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    static StudentView toView(Student s) {
        return new StudentView(s.getId(), s.getFirstName(), s.getLastName(), s.fullName(),
                s.getEmail(), s.getDateOfBirth(), s.getGuardianName(), s.getGuardianEmail(),
                s.getGuardianPhone(), s.getStatus().name());
    }
}