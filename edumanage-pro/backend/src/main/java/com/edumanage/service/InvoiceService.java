package com.edumanage.service;

import com.edumanage.domain.Invoice;
import com.edumanage.domain.User;
import com.edumanage.repository.InvoiceRepository;
import com.edumanage.repository.StudentRepository;
import com.edumanage.tenant.TenantContext;
import com.edumanage.web.dto.CommonDtos.PageResponse;
import com.edumanage.web.dto.InvoiceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final StripeService stripeService;

    @Transactional(readOnly = true)
    public PageResponse<InvoiceView> list(int page, int size, String status, String studentId) {
        String tenantId = TenantContext.requireTenantId();
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "issueDate"));

        if (studentId != null && !studentId.isBlank()) {
            return PageResponse.from(
                    invoiceRepository.findAllByTenantIdAndStudentId(tenantId, studentId, pageable)
                            .map(InvoiceService::toView));
        }
        if (status != null && !status.isBlank()) {
            Invoice.Status parsed = parseStatus(status);
            return PageResponse.from(
                    invoiceRepository.findAllByTenantIdAndStatus(tenantId, parsed, pageable)
                            .map(InvoiceService::toView));
        }
        return PageResponse.from(
                invoiceRepository.findAllByTenantId(tenantId, pageable).map(InvoiceService::toView));
    }

    @Transactional(readOnly = true)
    public InvoiceView get(String id, User viewer) { return toView(requireVisibleTo(id, viewer)); }

    @Transactional
    public InvoiceView create(CreateInvoiceRequest req) {
        String tenantId = TenantContext.requireTenantId();
        studentRepository.findByIdAndTenantId(req.studentId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        LocalDate issue = req.issueDate() == null ? LocalDate.now() : req.issueDate();
        LocalDate due  = req.dueDate()   == null ? issue.plusDays(30) : req.dueDate();
        if (due.isBefore(issue)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueDate must be on/after issueDate");
        }

        Invoice invoice = Invoice.builder()
                .number(nextNumber(tenantId))
                .studentId(req.studentId())
                .description(req.description().trim())
                .amount(req.amount())
                .currency(req.currency() == null ? "USD" : req.currency().toUpperCase(Locale.ROOT))
                .status(Invoice.Status.SENT)
                .issueDate(issue)
                .dueDate(due)
                .build();

        return toView(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceView voidInvoice(String id) {
        Invoice invoice = require(id);
        if (invoice.getStatus() == Invoice.Status.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot void a paid invoice");
        }
        invoice.setStatus(Invoice.Status.VOID);
        return toView(invoiceRepository.save(invoice));
    }

    /** Idempotent: repeated webhook deliveries will not double-apply. */
    @Transactional
    public void markPaid(String invoiceId, String tenantId, String paymentIntentId) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (invoice.getStatus() == Invoice.Status.PAID) return;

        invoice.setStatus(Invoice.Status.PAID);
        invoice.setPaidAt(Instant.now());
        invoice.setStripePaymentIntentId(paymentIntentId);
        invoiceRepository.save(invoice);
    }

    @Transactional
    public CheckoutResponse createCheckoutSession(String invoiceId, User payer) {
        String tenantId = TenantContext.requireTenantId();
        Invoice invoice = requireVisibleTo(invoiceId, payer);

        if (invoice.getStatus() == Invoice.Status.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice already paid");
        }
        if (invoice.getStatus() == Invoice.Status.VOID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is void");
        }

        String url = stripeService.createCheckoutSession(invoice, tenantId);
        return new CheckoutResponse(invoice.getId(), url);
    }

    @Transactional
    public void attachCheckoutSession(String invoiceId, String sessionId) {
        Invoice invoice = require(invoiceId);
        invoice.setStripeCheckoutSessionId(sessionId);
        invoiceRepository.save(invoice);
    }

    /**
     * Staff can see any invoice in their school; a student only their own. Another
     * student's invoice is reported as not found, so ids cannot be probed.
     */
    private Invoice requireVisibleTo(String id, User viewer) {
        Invoice invoice = require(id);
        if (viewer.getRole() == User.Role.STUDENT
                && (viewer.getStudentId() == null || !viewer.getStudentId().equals(invoice.getStudentId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }
        return invoice;
    }

    private Invoice require(String id) {
        return invoiceRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    private String nextNumber(String tenantId) {
        String prefix = "INV-" + Year.now().getValue() + "-";
        long count = invoiceRepository.countByTenantIdAndNumberStartingWith(tenantId, prefix);
        return prefix + String.format("%05d", count + 1);
    }

    private static Invoice.Status parseStatus(String raw) {
        try {
            return Invoice.Status.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status: " + raw);
        }
    }

    static InvoiceView toView(Invoice i) {
        return new InvoiceView(i.getId(), i.getNumber(), i.getStudentId(), i.getDescription(),
                i.getAmount(), i.getCurrency(), i.getStatus().name(),
                i.getIssueDate(), i.getDueDate(), i.getPaidAt());
    }
}