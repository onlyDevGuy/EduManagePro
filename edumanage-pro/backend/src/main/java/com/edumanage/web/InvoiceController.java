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
        // Students may only ever see their own invoices.
        if (user.getRole() == User.Role.STUDENT) {
            studentId = user.getStudentId();
        }
        return invoiceService.list(page, size, status, studentId);
    }

    @GetMapping("/{id}")
    public InvoiceView get(@PathVariable String id) {
        return invoiceService.get(id);
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
    public CheckoutResponse checkout(@PathVariable String id) {
        return invoiceService.createCheckoutSession(id);
    }
}
