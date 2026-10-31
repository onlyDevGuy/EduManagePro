package com.edumanage;

import com.edumanage.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillingTest extends IntegrationTest {

    @Test
    void invoicesAreNumberedInSequencePerSchool() throws Exception {
        School school = registerSchool();
        String student = createStudent(school, "Maya");
        String first = createInvoice(school, student, "100.00");
        String second = createInvoice(school, student, "200.00");

        String year = String.valueOf(java.time.Year.now().getValue());
        call(get("/api/v1/invoices/" + first), school.adminToken()).andExpect(jsonPath("$.number").value("INV-" + year + "-00001"));
        call(get("/api/v1/invoices/" + second), school.adminToken()).andExpect(jsonPath("$.number").value("INV-" + year + "-00002"));

        // Another school starts its own sequence.
        School other = registerSchool();
        String otherInvoice = createInvoice(other, createStudent(other, "Ben"), "50.00");
        call(get("/api/v1/invoices/" + otherInvoice), other.adminToken()).andExpect(jsonPath("$.number").value("INV-" + year + "-00001"));
    }

    @Test
    void invoiceRulesAreEnforced() throws Exception {
        School school = registerSchool();
        String student = createStudent(school, "Liam");

        call(post("/api/v1/invoices"), school.adminToken(),
                Map.of("studentId", student, "description", "Too small", "amount", "0.10"))
                .andExpect(status().isBadRequest());
        call(post("/api/v1/invoices"), school.adminToken(), Map.of("studentId", student, "description", "Backwards",
                "amount", "10.00", "issueDate", "2026-02-01", "dueDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("dueDate must be on/after issueDate"));
    }

    @Test
    void stripeWebhookWithAValidSignatureMarksTheInvoicePaidOnce() throws Exception {
        School school = registerSchool();
        String invoice = createInvoice(school, createStudent(school, "Sofia"), "1500.00");
        String event = checkoutCompleted(invoice, school.tenantId());

        webhook(event, sign(event, WEBHOOK_SECRET)).andExpect(status().isOk());
        call(get("/api/v1/invoices/" + invoice), school.adminToken())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());

        // Stripe retries deliveries; a repeat must not fail or change anything.
        webhook(event, sign(event, WEBHOOK_SECRET)).andExpect(status().isOk());

        // A paid invoice can no longer be voided.
        call(post("/api/v1/invoices/" + invoice + "/void"), school.adminToken()).andExpect(status().isConflict());
    }

    @Test
    void stripeWebhookWithABadSignatureIsRejectedAndChangesNothing() throws Exception {
        School school = registerSchool();
        String invoice = createInvoice(school, createStudent(school, "Kai"), "900.00");
        String event = checkoutCompleted(invoice, school.tenantId());

        webhook(event, sign(event, "whsec_attacker_guess")).andExpect(status().isBadRequest());
        call(get("/api/v1/invoices/" + invoice), school.adminToken()).andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void stripeWebhookWithoutASignatureIsRejected() throws Exception {
        mvc.perform(post("/api/v1/webhooks/stripe").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions webhook(String payload, String signature) throws Exception {
        return mvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signature)
                .content(payload));
    }

    /** The Stripe-Signature header: HMAC-SHA256 of "timestamp.payload" with the endpoint secret. */
    private static String sign(String payload, String secret) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + signature;
    }

    private static String checkoutCompleted(String invoiceId, String tenantId) {
        return """
            {"id":"evt_test_1","object":"event","api_version":"2024-06-20","created":%d,
             "type":"checkout.session.completed","livemode":false,"pending_webhooks":1,
             "data":{"object":{"id":"cs_test_1","object":"checkout.session","payment_intent":"pi_test_1",
               "metadata":{"invoiceId":"%s","tenantId":"%s"}}}}
            """.formatted(Instant.now().getEpochSecond(), invoiceId, tenantId);
    }
}
