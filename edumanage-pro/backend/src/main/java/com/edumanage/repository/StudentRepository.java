package com.edumanage.repository;

import com.edumanage.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, String> {
    List<Student> findByTenantId(String tenantId);
    
    Page<Student> findAllByTenantId(String tenantId, Pageable pageable);
    
    @Query("SELECT s FROM Student s WHERE s.tenantId = :tenantId AND (LOWER(s.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Student> search(@Param("tenantId") String tenantId, @Param("q") String q, Pageable pageable);
    
    Optional<Student> findByIdAndTenantId(String id, String tenantId);
    
    boolean existsByEmailAndTenantId(String email, String tenantId);
}
