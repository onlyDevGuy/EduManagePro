import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useAuth } from "../lib/auth";

interface Course {
  id: string; code: string; title: string; description: string | null;
  feeAmount: number; currency: string; active: boolean;
}
interface Student { id: string; fullName: string; }
interface Enrollment { id: string; studentId: string; courseId: string; status: string; enrolledOn: string; }
interface Page<T> { items: T[]; totalItems: number; }

const emptyForm = { code: "", title: "", description: "", feeAmount: "0", currency: "USD" };

export default function Courses() {
  const qc = useQueryClient();
  const { hasRole } = useAuth();
  const staff = hasRole("ADMIN", "TEACHER");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

  const courses = useQuery({
    queryKey: ["courses"],
    queryFn: () => api<Page<Course>>("/api/v1/courses?page=0&size=100"),
  });

  const create = useMutation({
    mutationFn: () => api<Course>("/api/v1/courses", {
      method: "POST",
      body: JSON.stringify({
        code: form.code, title: form.title,
        description: form.description || null,
        feeAmount: Number(form.feeAmount), currency: form.currency,
      }),
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["courses"] });
      setForm(emptyForm); setShowForm(false); setError(null);
    },
    onError: (e) => setError(e instanceof ApiError ? e.message : "Failed to create course"),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Courses</h1>
          <p className="text-sm text-slate-500">{courses.data?.totalItems ?? 0} courses</p>
        </div>
        {staff && (
          <button onClick={() => setShowForm((v) => !v)}
                  className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700">
            {showForm ? "Cancel" : "Add course"}
          </button>
        )}
      </div>

      {error && <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">{error}</div>}

      {showForm && staff && (
        <form onSubmit={(e) => { e.preventDefault(); create.mutate(); }}
              className="grid grid-cols-1 sm:grid-cols-2 gap-3 rounded-lg border border-slate-200 bg-white p-4">
          <label className="text-sm">
            <span className="block text-slate-600 mb-1">Code</span>
            <input required value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })}
                   className="w-full rounded-md border border-slate-300 px-3 py-1.5" placeholder="MATH-101" />
          </label>
          <label className="text-sm">
            <span className="block text-slate-600 mb-1">Title</span>
            <input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}
                   className="w-full rounded-md border border-slate-300 px-3 py-1.5" placeholder="Algebra I" />
          </label>
          <label className="text-sm">
            <span className="block text-slate-600 mb-1">Fee amount</span>
            <input required type="number" min="0" step="0.01" value={form.feeAmount}
                   onChange={(e) => setForm({ ...form, feeAmount: e.target.value })}
                   className="w-full rounded-md border border-slate-300 px-3 py-1.5" />
          </label>
          <label className="text-sm">
            <span className="block text-slate-600 mb-1">Currency</span>
            <input required maxLength={3} value={form.currency}
                   onChange={(e) => setForm({ ...form, currency: e.target.value.toUpperCase() })}
                   className="w-full rounded-md border border-slate-300 px-3 py-1.5" />
          </label>
          <label className="text-sm sm:col-span-2">
            <span className="block text-slate-600 mb-1">Description</span>
            <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })}
                   className="w-full rounded-md border border-slate-300 px-3 py-1.5" />
          </label>
          <div className="sm:col-span-2">
            <button type="submit" disabled={create.isPending}
                    className="rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50">
              {create.isPending ? "Saving…" : "Save course"}
            </button>
          </div>
        </form>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {courses.data?.items.map((c) => (
          <div key={c.id} className="rounded-lg border border-slate-200 bg-white p-4">
            <div className="flex items-start justify-between">
              <div>
                <div className="font-mono text-xs text-slate-500">{c.code}</div>
                <div className="font-semibold text-slate-900">{c.title}</div>
                {c.description && <p className="mt-1 text-sm text-slate-500">{c.description}</p>}
              </div>
              <div className="text-right">
                <div className="text-sm font-medium text-slate-900">{c.currency} {Number(c.feeAmount).toFixed(2)}</div>
                <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-xs ${c.active ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>
                  {c.active ? "Active" : "Inactive"}
                </span>
              </div>
            </div>
            {staff && (
              <button onClick={() => setSelected(selected === c.id ? null : c.id)}
                      className="mt-3 text-xs font-medium text-indigo-600 hover:underline">
                {selected === c.id ? "Hide roster" : "Manage roster"}
              </button>
            )}
            {staff && selected === c.id && <Roster courseId={c.id} />}
          </div>
        ))}
        {courses.isLoading && <div className="text-sm text-slate-500">Loading…</div>}
        {!courses.isLoading && courses.data?.items.length === 0 && (
          <div className="text-sm text-slate-500">No courses yet.</div>
        )}
      </div>
    </div>
  );
}

function Roster({ courseId }: { courseId: string }) {
  const qc = useQueryClient();
  const [studentId, setStudentId] = useState("");

  const enrollments = useQuery({
    queryKey: ["enrollments", courseId],
    queryFn: () => api<Enrollment[]>(`/api/v1/courses/${courseId}/enrollments`),
  });
  const students = useQuery({
    queryKey: ["students", "all"],
    queryFn: () => api<Page<Student>>("/api/v1/students?page=0&size=100"),
  });

  const nameOf = (id: string) => students.data?.items.find((s) => s.id === id)?.fullName ?? id;

  const enroll = useMutation({
    mutationFn: () => api(`/api/v1/courses/${courseId}/enrollments`, {
      method: "POST", body: JSON.stringify({ studentId }),
    }),
    onSuccess: () => { setStudentId(""); qc.invalidateQueries({ queryKey: ["enrollments", courseId] }); },
    onError: (e) => alert(e instanceof ApiError ? e.message : "Could not enroll"),
  });

  const unenroll = useMutation({
    mutationFn: (sid: string) => api<void>(`/api/v1/courses/${courseId}/enrollments/${sid}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["enrollments", courseId] }),
  });

  const active = (enrollments.data ?? []).filter((e) => e.status === "ACTIVE");

  return (
    <div className="mt-3 border-t border-slate-100 pt-3">
      <div className="flex gap-2">
        <select value={studentId} onChange={(e) => setStudentId(e.target.value)}
                className="flex-1 rounded-md border border-slate-300 px-2 py-1 text-sm">
          <option value="">Select a student…</option>
          {students.data?.items.map((s) => <option key={s.id} value={s.id}>{s.fullName}</option>)}
        </select>
        <button disabled={!studentId || enroll.isPending} onClick={() => enroll.mutate()}
                className="rounded-md bg-indigo-600 px-3 py-1 text-sm text-white hover:bg-indigo-700 disabled:opacity-40">
          Enroll
        </button>
      </div>
      <ul className="mt-3 space-y-1 text-sm">
        {active.map((e) => (
          <li key={e.id} className="flex items-center justify-between">
            <span className="text-slate-700">{nameOf(e.studentId)}</span>
            <button onClick={() => unenroll.mutate(e.studentId)} className="text-xs text-red-600 hover:underline">
              Remove
            </button>
          </li>
        ))}
        {active.length === 0 && <li className="text-slate-400">No students enrolled.</li>}
      </ul>
    </div>
  );
}
