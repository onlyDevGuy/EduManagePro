package com.edumanage.tenant;

/**
 * Thread-scoped tenant binding. Populated by {@code JwtAuthenticationFilter} from the
 * {@code tid} claim, or explicitly during registration/seeding.
 *
 * NOTE: not propagated to @Async / @Scheduled threads — see roadmap item T-14.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT.get();
    }

    public static String requireTenantId() {
        String id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("No tenant bound to the current thread");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}