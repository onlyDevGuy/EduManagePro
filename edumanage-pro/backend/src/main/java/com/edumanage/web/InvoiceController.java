package com.edumanage.web;

import com.edumanage.domain.User;
import com.edumanage.service.InvoiceService;
import com.edumanage.web.dto.CommonDtos.PageResponse;
import com.edumanage.web.dto.InvoiceDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public PageResponse<InvoiceView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentId,
            @AuthenticationPrincipal User user) {
        // Students may only ever see their own invoices. A student login that is not
        // linked to a student record sees none (not, by falling through, everyone's).
        if (user.getRole() == User.Role.STUDENT) {
            if (user.getStudentId() == null) return PageResponse.empty(page, size);
            studentId = user.getStudentId();
        }
        return invoiceService.list(page, size, status, studentId);
    }

    @GetMapping("/{id}")
    public InvoiceView get(@PathVariable String id, @AuthenticationPrincipal User user) {
        return invoiceService.get(id, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceView create(@Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.create(request);
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceView voidInvoice(@PathVariable String id) {
        return invoiceService.voidInvoice(id);
    }

    @PostMapping("/{id}/checkout-session")
    public CheckoutResponse checkout(@PathVariable String id, @AuthenticationPrincipal User user) {
        return invoiceService.createCheckoutSession(id, user);
    }
}
