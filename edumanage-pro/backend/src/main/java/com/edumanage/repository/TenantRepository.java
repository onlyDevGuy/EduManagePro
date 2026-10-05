package com.edumanage.repository;

import com.edumanage.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
