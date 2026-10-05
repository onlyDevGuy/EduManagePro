import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useAuth } from "../lib/auth";

interface Student {
  id: string; firstName: string; lastName: string; fullName: string;
  email: string | null; guardianName: string | null;
  guardianEmail: string | null; guardianPhone: string | null; status: string;
}
interface Page<T> { items: T[]; page: number; totalPages: number; totalItems: number; hasNext: boolean; }

const emptyForm = { firstName: "", lastName: "", email: "", guardianName: "", guardianEmail: "", guardianPhone: "" };

export default function Students() {
  const qc = useQueryClient();
  const { hasRole } = useAuth();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["students", page, q],
    queryFn: () => api<Page<Student>>(
      `/api/v1/students?page=${page}&size=20${q ? `&q=${encodeURIComponent(q)}` : ""}`),
  });

  const create = useMutation({
    mutationFn: (body: typeof emptyForm) =>
      api<Student>("/api/v1/students", { method: "POST", body: JSON.stringify(body) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["students"] });
      setForm(emptyForm); setShowForm(false); setError(null);
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "Failed to create student"),
  });

  const remove = useMutation({
    mutationFn: (id: string) => api<void>(`/api/v1/students/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["students"] }),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Students</h1>
          <p className="text-sm text-slate-500">{data?.totalItems ?? 0} enrolled records</p>
        </div>
        <div className="flex gap-2">
          <input
            value={q}
            onChange={(e) => { setQ(e.target.value); setPage(0); }}
            placeholder="Search name or email…"
            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm w-56"
          />
          {hasRole("ADMIN", "TEACHER") && (
            <button
              onClick={() => setShowForm(v => !v)}
              className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700"
            >
              {showForm ? "Cancel" : "Add student"}
            </button>
          )}
        </div>
      </div>

      {error && <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">{error}</div>}

      {showForm && (
        <form
          onSubmit={(e) => { e.preventDefault(); create.mutate(form); }}
          className="grid grid-cols-1 sm:grid-cols-3 gap-3 rounded-lg border border-slate-200 bg-white p-4"
        >
          {(["firstName","lastName","email","guardianName","guardianEmail","guardianPhone"] as const).map(f => (
            <label key={f} className="text-sm">
              <span className="block text-slate-600 capitalize mb-1">
                {f.replace(/([A-Z])/g, " $1").toLowerCase()}
              </span>
              <input
                required={f === "firstName" || f === "lastName"}
                value={(form as any)[f]}
                onChange={(e) => setForm({ ...form, [f]: e.target.value })}
                className="w-full rounded-md border border-slate-300 px-3 py-1.5"
              />
            </label>
          ))}
          <div className="sm:col-span-3">
            <button
              type="submit"
              disabled={create.isPending}
              className="rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {create.isPending ? "Saving…" : "Save student"}
            </button>
          </div>
        </form>
      )}

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-slate-600">
            <tr>
              <th className="px-4 py-2 text-left font-medium">Name</th>
              <th className="px-4 py-2 text-left font-medium">Email</th>
              <th className="px-4 py-2 text-left font-medium">Guardian</th>
              <th className="px-4 py-2 text-left font-medium">Status</th>
              <th className="px-4 py-2" />
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading && (
              <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-500">Loading…</td></tr>
            )}
            {data?.items.map((s: Student) => (
              <tr key={s.id} className="hover:bg-slate-50">
                <td className="px-4 py-2 font-medium text-slate-900">{s.fullName}</td>
                <td className="px-4 py-2 text-slate-600">{s.email ?? "—"}</td>
                <td className="px-4 py-2 text-slate-600">
                  {s.guardianName ?? "—"}
                  {s.guardianPhone && <span className="block text-xs text-slate-400">{s.guardianPhone}</span>}
                </td>
                <td className="px-4 py-2">
                  <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700">
                    {s.status}
                  </span>
                </td>
                <td className="px-4 py-2 text-right">
                  {hasRole("ADMIN") && (
                    <button
                      onClick={() => remove.mutate(s.id)}
                      className="text-xs text-red-600 hover:underline"
                    >
                      Delete
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {!isLoading && data?.items.length === 0 && (
              <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-500">No students found.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-sm text-slate-600">
        <button
          disabled={page === 0}
          onClick={() => setPage(p => p - 1)}
          className="rounded-md border border-slate-300 px-3 py-1.5 disabled:opacity-40"
        >
          Previous
        </button>
        <span>Page {page + 1} of {Math.max(data?.totalPages ?? 1, 1)}</span>
        <button
          disabled={!data?.hasNext}
          onClick={() => setPage(p => p + 1)}
          className="rounded-md border border-slate-300 px-3 py-1.5 disabled:opacity-40"
        >
          Next
        </button>
      </div>
    </div>
  );
}
