package com.edumanage.bootstrap;

import com.edumanage.config.AppProperties;
import com.edumanage.domain.*;
import com.edumanage.repository.*;
import com.edumanage.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds a demo school so the deployed app is immediately explorable.
 * Disable with BOOTSTRAP_ENABLED=false.
 *
 * Demo login ->  tenant: demo-academy / admin@demo-academy.test / Password123!
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private final AppProperties props;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.bootstrap().enabled() || tenantRepository.existsBySlug("demo-academy")) {
            return;
        }
        log.info("Seeding demo tenant 'demo-academy' ...");

        Tenant tenant = tenantRepository.save(
                Tenant.builder().name("Demo Academy").slug("demo-academy").build());

        try {
            TenantContext.setTenantId(tenant.getId());

            User admin = userRepository.save(user("admin@demo-academy.test", "Ada Admin", User.Role.ADMIN, null));
            userRepository.save(user("teacher@demo-academy.test", "Tom Teacher", User.Role.TEACHER, null));

            List<Student> students = studentRepository.saveAll(List.of(
                    Student.builder().firstName("Maya").lastName("Chen").email("maya.chen@example.test")
                            .dateOfBirth(LocalDate.of(2011, 4, 12))
                            .guardianName("Wei Chen").guardianEmail("wei.chen@example.test")
                            .guardianPhone("+1-555-0101").build(),
                    Student.builder().firstName("Liam").lastName("Okafor").email("liam.okafor@example.test")
                            .dateOfBirth(LocalDate.of(2010, 9, 3))
                            .guardianName("Ngozi Okafor").guardianEmail("ngozi@example.test")
                            .guardianPhone("+1-555-0102").build(),
                    Student.builder().firstName("Sofia").lastName("Rossi").email("sofia.rossi@example.test")
                            .dateOfBirth(LocalDate.of(2012, 1, 27))
                            .guardianName("Marco Rossi").guardianEmail("marco@example.test")
                            .guardianPhone("+1-555-0103").build()));

            Course math = courseRepository.save(Course.builder()
                    .code("MATH-101").title("Algebra I")
                    .description("Foundations of algebra: expressions, equations, functions.")
                    .teacherId(admin.getId()).feeAmount(new BigDecimal("450.00")).currency("USD")
                    .startDate(LocalDate.now().minusWeeks(4)).endDate(LocalDate.now().plusMonths(4))
                    .build());

            Course science = courseRepository.save(Course.builder()
                    .code("SCI-201").title("General Science")
                    .description("Integrated physics, chemistry, and biology for middle school.")
                    .teacherId(admin.getId()).feeAmount(new BigDecimal("520.00")).currency("USD")
                    .startDate(LocalDate.now().minusWeeks(2)).endDate(LocalDate.now().plusMonths(4))
                    .build());

            for (Student s : students) {
                enrollmentRepository.save(Enrollment.builder()
                        .studentId(s.getId()).courseId(math.getId())
                        .status(Enrollment.Status.ACTIVE).enrolledOn(LocalDate.now().minusWeeks(4)).build());
                enrollmentRepository.save(Enrollment.builder()
                        .studentId(s.getId()).courseId(science.getId())
                        .status(Enrollment.Status.ACTIVE).enrolledOn(LocalDate.now().minusWeeks(2)).build());
            }

            LocalDate session = LocalDate.now().minusDays(3);
            attendanceRepository.saveAll(students.stream().map(s -> AttendanceRecord.builder()
                    .courseId(math.getId()).studentId(s.getId()).sessionDate(session)
                    .status(s.getLastName().equals("Okafor")
                            ? AttendanceRecord.Status.LATE
                            : AttendanceRecord.Status.PRESENT)
                    .recordedBy(admin.getId()).build()).toList());

            invoiceRepository.saveAll(List.of(
                    Invoice.builder().number("INV-2025-00001").studentId(students.get(0).getId())
                            .description("Algebra I — Term 1 tuition")
                            .amount(new BigDecimal("450.00")).currency("USD")
                            .status(Invoice.Status.SENT)
                            .issueDate(LocalDate.now().minusDays(10)).dueDate(LocalDate.now().plusDays(20)).build(),
                    Invoice.builder().number("INV-2025-00002").studentId(students.get(1).getId())
                            .description("General Science — Term 1 tuition + lab fee")
                            .amount(new BigDecimal("560.00")).currency("USD")
                            .status(Invoice.Status.OVERDUE)
                            .issueDate(LocalDate.now().minusDays(45)).dueDate(LocalDate.now().minusDays(5)).build(),
                    Invoice.builder().number("INV-2025-00003").studentId(students.get(2).getId())
                            .description("Algebra I — Term 1 tuition")
                            .amount(new BigDecimal("450.00")).currency("USD")
                            .status(Invoice.Status.PAID).paidAt(java.time.Instant.now())
                            .issueDate(LocalDate.now().minusDays(30)).dueDate(LocalDate.now()).build()));

            log.info("Demo seed complete. Login with tenant=demo-academy, admin@demo-academy.test / Password123!");
        } finally {
            TenantContext.clear();
        }
    }

    private User user(String email, String name, User.Role role, String studentId) {
        return User.builder()
                .email(email)
                .fullName(name)
                .role(role)
                .studentId(studentId)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .enabled(true)
                .build();
    }
}