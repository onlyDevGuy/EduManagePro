package com.edumanage.domain;

import com.edumanage.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(TenantAwareEntity.AuditListener.class)
public abstract class TenantAwareEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static class AuditListener {
        @PrePersist
        void prePersist(TenantAwareEntity e) {
            if (e.getId() == null) {
                e.setId(UUID.randomUUID().toString());
            }
            if (e.getTenantId() == null) {
                e.setTenantId(TenantContext.requireTenantId());
            }
            Instant now = Instant.now();
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
        }

        @PreUpdate
        void preUpdate(TenantAwareEntity e) {
            e.setUpdatedAt(Instant.now());
        }
    }
}