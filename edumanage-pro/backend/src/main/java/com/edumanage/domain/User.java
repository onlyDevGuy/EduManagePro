package com.edumanage.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users",
       uniqueConstraints = @UniqueConstraint(name = "uk_users_tenant_email",
                                             columnNames = {"tenant_id", "email"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends TenantAwareEntity implements UserDetails {

    public enum Role { ADMIN, TEACHER, STUDENT }

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Set when role == STUDENT, links the login to the student record. */
    @Column(name = "student_id", length = 36)
    private String studentId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword()  { return passwordHash; }
    @Override public String getUsername()  { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return enabled; }
}