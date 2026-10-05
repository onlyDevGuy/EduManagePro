package com.edumanage.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class InvoiceDtos {

    private InvoiceDtos() {}

    public record CreateInvoiceRequest(
            @NotBlank String studentId,
            @NotBlank @Size(max = 300) String description,
            @NotNull @DecimalMin(value = "0.50") BigDecimal amount,
            @Size(min = 3, max = 3) String currency,
            LocalDate issueDate,
            LocalDate dueDate) {}

    public record InvoiceView(String id, String number, String studentId, String description,
                              BigDecimal amount, String currency, String status,
                              LocalDate issueDate, LocalDate dueDate, Instant paidAt) {}

    public record CheckoutResponse(String invoiceId, String checkoutUrl) {}
}