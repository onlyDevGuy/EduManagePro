package com.edumanage;

import com.edumanage.domain.User;
import com.edumanage.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Schools are isolated from each other, and roles limit what each user can see and do. */
class AccessControlTest extends IntegrationTest {

    // -- between schools ------------------------------------------------------

    @Test
    void oneSchoolCannotReadOrChangeAnothersStudents() throws Exception {
        School a = registerSchool();
        School b = registerSchool();
        String studentOfA = createStudent(a, "Maya");

        call(get("/api/v1/students/" + studentOfA), b.adminToken()).andExpect(status().isNotFound());
        call(put("/api/v1/students/" + studentOfA), b.adminToken(), Map.of("firstName", "Hacked"))
                .andExpect(status().isNotFound());
        call(delete("/api/v1/students/" + studentOfA), b.adminToken()).andExpect(status().isNotFound());
        call(get("/api/v1/students"), b.adminToken()).andExpect(jsonPath("$.totalItems").value(0));

        // Still intact for its own school.
        call(get("/api/v1/students/" + studentOfA), a.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Maya"));
    }

    @Test
    void oneSchoolCannotSeeOrBillAnothersInvoicesOrStudents() throws Exception {
        School a = registerSchool();
        School b = registerSchool();
        String studentOfA = createStudent(a, "Liam");
        String invoiceOfA = createInvoice(a, studentOfA, "1500.00");

        call(get("/api/v1/invoices/" + invoiceOfA), b.adminToken()).andExpect(status().isNotFound());
        call(post("/api/v1/invoices/" + invoiceOfA + "/void"), b.adminToken()).andExpect(status().isNotFound());
        // Nor raise an invoice against a student from another school.
        call(post("/api/v1/invoices"), b.adminToken(),
                Map.of("studentId", studentOfA, "description", "Sneaky", "amount", "10.00"))
                .andExpect(status().isNotFound());
    }

    // -- roles within a school ------------------------------------------------

    @Test
    void teachersManageStudentsButOnlyAdminsDelete() throws Exception {
        School school = registerSchool();
        String teacher = userToken(school, User.Role.TEACHER, null);
        String student = createStudent(school, "Sofia");

        call(put("/api/v1/students/" + student), teacher, Map.of("lastName", "Rossi")).andExpect(status().isOk());
        call(delete("/api/v1/students/" + student), teacher)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You do not have permission to do this."));
        call(delete("/api/v1/students/" + student), school.adminToken()).andExpect(status().isNoContent());
    }

    @Test
    void onlyAdminsHandleBilling() throws Exception {
        School school = registerSchool();
        String teacher = userToken(school, User.Role.TEACHER, null);
        String student = createStudent(school, "Noah");

        call(post("/api/v1/invoices"), teacher, Map.of("studentId", student, "description", "Fees", "amount", "100.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentsCannotBrowseOtherStudentsRecords() throws Exception {
        School school = registerSchool();
        String me = createStudent(school, "Amara");
        String classmate = createStudent(school, "Ben");
        String myLogin = userToken(school, User.Role.STUDENT, me);

        call(get("/api/v1/students"), myLogin).andExpect(status().isForbidden());
        call(get("/api/v1/students/" + classmate), myLogin).andExpect(status().isForbidden());
        call(get("/api/v1/students/" + me), myLogin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Amara"));
    }

    @Test
    void studentsOnlySeeAndPayTheirOwnInvoices() throws Exception {
        School school = registerSchool();
        String me = createStudent(school, "Zola");
        String classmate = createStudent(school, "Kai");
        String mine = createInvoice(school, me, "800.00");
        String theirs = createInvoice(school, classmate, "950.00");
        String myLogin = userToken(school, User.Role.STUDENT, me);

        call(get("/api/v1/invoices"), myLogin)
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(mine));
        // Asking for someone else's student id still only returns your own.
        call(get("/api/v1/invoices?studentId=" + classmate), myLogin)
                .andExpect(jsonPath("$.items[0].id").value(mine));

        call(get("/api/v1/invoices/" + mine), myLogin).andExpect(status().isOk());
        call(get("/api/v1/invoices/" + theirs), myLogin).andExpect(status().isNotFound());
        call(post("/api/v1/invoices/" + theirs + "/checkout-session"), myLogin).andExpect(status().isNotFound());
    }

    @Test
    void aStudentLoginWithoutAStudentRecordSeesNoInvoices() throws Exception {
        School school = registerSchool();
        createInvoice(school, createStudent(school, "Thandi"), "500.00");
        String unlinked = userToken(school, User.Role.STUDENT, null);

        call(get("/api/v1/invoices"), unlinked).andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void studentsCannotSeeRostersOrAttendance() throws Exception {
        School school = registerSchool();
        String me = createStudent(school, "Lebo");
        String myLogin = userToken(school, User.Role.STUDENT, me);
        String courseId = read(call(post("/api/v1/courses"), school.adminToken(),
                Map.of("code", "MATH-101", "title", "Mathematics", "feeAmount", "0.00"))
                .andExpect(status().isCreated())).get("id").asText();

        call(get("/api/v1/courses"), myLogin).andExpect(status().isOk());
        call(get("/api/v1/courses/" + courseId + "/enrollments"), myLogin).andExpect(status().isForbidden());
        call(get("/api/v1/attendance/students/" + me + "/summary"), myLogin).andExpect(status().isForbidden());
        call(post("/api/v1/attendance/courses/" + courseId + "/sessions"), myLogin, Map.of(
                "sessionDate", LocalDate.of(2026, 1, 15).toString(),
                "entries", List.of(Map.of("studentId", me, "status", "PRESENT"))))
                .andExpect(status().isForbidden());
    }
}
