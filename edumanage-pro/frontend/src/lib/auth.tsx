import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api, tokenStore } from "./api";

export type Role = "ADMIN" | "TEACHER" | "STUDENT";

export interface SessionUser {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  studentId?: string | null;
}

export interface TenantInfo { id: string; name: string; slug: string; }

interface AuthState {
  user: SessionUser | null;
  tenant: TenantInfo | null;
  loading: boolean;
  login: (tenantSlug: string, email: string, password: string) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
  hasRole: (...roles: Role[]) => boolean;
}

export interface RegisterPayload {
  schoolName: string;
  slug: string;
  adminEmail: string;
  adminPassword: string;
  adminFullName: string;
}

const SESSION_KEY = "em.session";
const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null);
  const [tenant, setTenant] = useState<TenantInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const raw = localStorage.getItem(SESSION_KEY);
    if (raw && tokenStore.access) {
      try {
        const parsed = JSON.parse(raw);
        setUser(parsed.user);
        setTenant(parsed.tenant);
      } catch { localStorage.removeItem(SESSION_KEY); }
    }
    setLoading(false);
  }, []);

  function persist(access: string, refresh: string, u: SessionUser, t: TenantInfo) {
    tokenStore.set(access, refresh);
    localStorage.setItem(SESSION_KEY, JSON.stringify({ user: u, tenant: t }));
    setUser(u);
    setTenant(t);
  }

  const value = useMemo<AuthState>(() => ({
    user,
    tenant,
    loading,
    async login(tenantSlug, email, password) {
      const res = await api<any>("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify({ tenantSlug, email, password }),
      });
      persist(res.accessToken, res.refreshToken, res.user, res.tenant);
    },
    async register(payload) {
      const res = await api<any>("/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      persist(res.accessToken, res.refreshToken, res.user, res.tenant);
    },
    logout() {
      tokenStore.clear();
      localStorage.removeItem(SESSION_KEY);
      setUser(null);
      setTenant(null);
    },
    hasRole: (...roles: Role[]) => !!user && roles.includes(user.role),
  }), [user, tenant, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}