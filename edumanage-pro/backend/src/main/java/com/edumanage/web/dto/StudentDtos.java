package com.edumanage.web.dto;

import com.edumanage.domain.Student;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public final class StudentDtos {

    private StudentDtos() {}

    public record CreateStudentRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Email @Size(max = 180) String email,
            LocalDate dateOfBirth,
            @Size(max = 200) String guardianName,
            @Email @Size(max = 180) String guardianEmail,
            @Size(max = 40)  String guardianPhone,
            Student.Status status) {}

    public record UpdateStudentRequest(
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Email @Size(max = 180) String email,
            LocalDate dateOfBirth,
            @Size(max = 200) String guardianName,
            @Email @Size(max = 180) String guardianEmail,
            @Size(max = 40)  String guardianPhone,
            Student.Status status) {}

    public record StudentView(String id, String firstName, String lastName, String fullName,
                              String email, LocalDate dateOfBirth, String guardianName,
                              String guardianEmail, String guardianPhone, String status) {}
}