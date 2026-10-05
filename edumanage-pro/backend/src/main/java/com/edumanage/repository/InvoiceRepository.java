package com.edumanage.repository;

import com.edumanage.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    Page<Invoice> findAllByTenantId(String tenantId, Pageable pageable);
    Page<Invoice> findAllByTenantIdAndStatus(String tenantId, Invoice.Status status, Pageable pageable);
    Page<Invoice> findAllByTenantIdAndStudentId(String tenantId, String studentId, Pageable pageable);
    Optional<Invoice> findByIdAndTenantId(String id, String tenantId);
    long countByTenantIdAndNumberStartingWith(String tenantId, String prefix);
}
