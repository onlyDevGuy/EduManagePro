import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";

interface Course { id: string; code: string; title: string; }
interface Student { id: string; fullName: string; }
interface Enrollment { studentId: string; status: string; }
interface Page<T> { items: T[]; }

type Status = "PRESENT" | "ABSENT" | "LATE" | "EXCUSED";
const STATUSES: Status[] = ["PRESENT", "ABSENT", "LATE", "EXCUSED"];
const STATUS_STYLE: Record<Status, string> = {
  PRESENT: "bg-emerald-600 text-white",
  ABSENT: "bg-red-600 text-white",
  LATE: "bg-amber-500 text-white",
  EXCUSED: "bg-slate-500 text-white",
};

export default function Attendance() {
  const today = new Date().toISOString().slice(0, 10);
  const [courseId, setCourseId] = useState("");
  const [date, setDate] = useState(today);
  const [marks, setMarks] = useState<Record<string, Status>>({});
  const [message, setMessage] = useState<string | null>(null);

  const courses = useQuery({
    queryKey: ["courses"],
    queryFn: () => api<Page<Course>>("/api/v1/courses?page=0&size=100"),
  });
  const enrollments = useQuery({
    queryKey: ["enrollments", courseId],
    queryFn: () => api<Enrollment[]>(`/api/v1/courses/${courseId}/enrollments`),
    enabled: !!courseId,
  });
  const students = useQuery({
    queryKey: ["students", "all"],
    queryFn: () => api<Page<Student>>("/api/v1/students?page=0&size=100"),
  });

  const roster = useMemo(() => {
    const active = (enrollments.data ?? []).filter((e) => e.status === "ACTIVE");
    return active.map((e) => ({
      studentId: e.studentId,
      name: students.data?.items.find((s) => s.id === e.studentId)?.fullName ?? e.studentId,
    }));
  }, [enrollments.data, students.data]);

  // Default everyone to PRESENT when the roster loads.
  useEffect(() => {
    if (roster.length) {
      setMarks((prev) => {
        const next = { ...prev };
        roster.forEach((r) => { if (!next[r.studentId]) next[r.studentId] = "PRESENT"; });
        return next;
      });
    }
  }, [roster]);

  const submit = useMutation({
    mutationFn: () => api(`/api/v1/attendance/courses/${courseId}/sessions`, {
      method: "POST",
      body: JSON.stringify({
        sessionDate: date,
        entries: roster.map((r) => ({ studentId: r.studentId, status: marks[r.studentId] ?? "PRESENT" })),
      }),
    }),
    onSuccess: (res: any) => setMessage(`Saved — ${res.created} new, ${res.updated} updated.`),
    onError: (e) => setMessage(e instanceof ApiError ? e.message : "Could not save attendance"),
  });

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">Attendance</h1>
        <p className="text-sm text-slate-500">Mark a class register for a session.</p>
      </div>

      <div className="flex flex-wrap gap-3 rounded-lg border border-slate-200 bg-white p-4">
        <label className="text-sm">
          <span className="block text-slate-600 mb-1">Course</span>
          <select value={courseId} onChange={(e) => { setCourseId(e.target.value); setMarks({}); setMessage(null); }}
                  className="rounded-md border border-slate-300 px-3 py-1.5 text-sm min-w-[14rem]">
            <option value="">Select a course…</option>
            {courses.data?.items.map((c) => <option key={c.id} value={c.id}>{c.code} — {c.title}</option>)}
          </select>
        </label>
        <label className="text-sm">
          <span className="block text-slate-600 mb-1">Session date</span>
          <input type="date" value={date} max={today} onChange={(e) => setDate(e.target.value)}
                 className="rounded-md border border-slate-300 px-3 py-1.5 text-sm" />
        </label>
      </div>

      {message && <div className="rounded-md bg-indigo-50 border border-indigo-200 p-3 text-sm text-indigo-800">{message}</div>}

      {courseId && (
        <div className="rounded-lg border border-slate-200 bg-white">
          <table className="min-w-full text-sm">
            <thead className="bg-slate-50 text-slate-600">
              <tr>
                <th className="px-4 py-2 text-left font-medium">Student</th>
                <th className="px-4 py-2 text-left font-medium">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {roster.map((r) => (
                <tr key={r.studentId}>
                  <td className="px-4 py-2 font-medium text-slate-900">{r.name}</td>
                  <td className="px-4 py-2">
                    <div className="flex gap-1">
                      {STATUSES.map((s) => {
                        const on = (marks[r.studentId] ?? "PRESENT") === s;
                        return (
                          <button key={s} onClick={() => setMarks((m) => ({ ...m, [r.studentId]: s }))}
                                  className={`rounded-md px-2.5 py-1 text-xs font-medium transition ${on ? STATUS_STYLE[s] : "bg-slate-100 text-slate-600 hover:bg-slate-200"}`}>
                            {s[0] + s.slice(1).toLowerCase()}
                          </button>
                        );
                      })}
                    </div>
                  </td>
                </tr>
              ))}
              {roster.length === 0 && (
                <tr><td colSpan={2} className="px-4 py-6 text-center text-slate-500">
                  {enrollments.isLoading ? "Loading roster…" : "No students enrolled in this course."}
                </td></tr>
              )}
            </tbody>
          </table>
          {roster.length > 0 && (
            <div className="border-t border-slate-100 p-3 text-right">
              <button onClick={() => submit.mutate()} disabled={submit.isPending}
                      className="rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50">
                {submit.isPending ? "Saving…" : "Save attendance"}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
