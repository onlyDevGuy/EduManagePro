package com.edumanage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student extends TenantAwareEntity {

    public enum Status { ACTIVE, INACTIVE, GRADUATED, WITHDRAWN }

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 180)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "guardian_name", length = 200)
    private String guardianName;

    @Column(name = "guardian_email", length = 180)
    private String guardianEmail;

    @Column(name = "guardian_phone", length = 40)
    private String guardianPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    public String fullName() {
        return firstName + " " + lastName;
    }
}