package com.edumanage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "invoices",
       uniqueConstraints = @UniqueConstraint(name = "uk_invoices_tenant_number",
                                             columnNames = {"tenant_id", "number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice extends TenantAwareEntity {

    public enum Status { DRAFT, SENT, PAID, OVERDUE, VOID }

    @Column(nullable = false, length = 40)
    private String number;

    @Column(name = "student_id", nullable = false, length = 36)
    private String studentId;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "stripe_checkout_session_id", length = 200)
    private String stripeCheckoutSessionId;

    @Column(name = "stripe_payment_intent_id", length = 200)
    private String stripePaymentIntentId;
}