const BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

const ACCESS_KEY = "em.accessToken";
const REFRESH_KEY = "em.refreshToken";

export const tokenStore = {
  get access() { return localStorage.getItem(ACCESS_KEY); },
  get refresh() { return localStorage.getItem(REFRESH_KEY); },
  set(access: string, refresh: string) {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

export class ApiError extends Error {
  constructor(public status: number, public detail: string) {
    super(detail);
  }
}

let refreshing: Promise<boolean> | null = null;

async function tryRefresh(): Promise<boolean> {
  const refreshToken = tokenStore.refresh;
  if (!refreshToken) return false;
  if (!refreshing) {
    refreshing = fetch(`${BASE}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (r) => {
        if (!r.ok) return false;
        const data = await r.json();
        tokenStore.set(data.accessToken, data.refreshToken);
        return true;
      })
      .catch(() => false)
      .finally(() => { refreshing = null; });
  }
  return refreshing;
}

export async function api<T>(
  path: string,
  init: RequestInit & { retry?: boolean } = {},
): Promise<T> {
  const { retry = true, ...rest } = init;
  const headers = new Headers(rest.headers);
  headers.set("Content-Type", "application/json");
  if (tokenStore.access) headers.set("Authorization", `Bearer ${tokenStore.access}`);

  const res = await fetch(`${BASE}${path}`, { ...rest, headers });

  if (res.status === 401 && retry && (await tryRefresh())) {
    return api<T>(path, { ...init, retry: false });
  }

  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.detail ?? body.title ?? detail;
    } catch { /* non-JSON error body */ }
    if (res.status === 401) tokenStore.clear();
    throw new ApiError(res.status, detail);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}