import { useQuery } from "@tanstack/react-query";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

interface Page<T> { items: T[]; totalItems: number; }
interface Invoice { amount: number; status: string; }

function StatCard({ label, value, hint, accent }: { label: string; value: string; hint?: string; accent: string }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className={`text-xs font-semibold uppercase tracking-wider ${accent}`}>{label}</div>
      <div className="mt-2 text-3xl font-bold text-slate-900">{value}</div>
      {hint && <div className="mt-1 text-sm text-slate-500">{hint}</div>}
    </div>
  );
}

export default function Dashboard() {
  const { user, tenant, hasRole } = useAuth();
  const staff = hasRole("ADMIN", "TEACHER");

  const students = useQuery({
    queryKey: ["dash", "students"],
    queryFn: () => api<Page<unknown>>("/api/v1/students?page=0&size=1"),
    enabled: staff,
  });
  const courses = useQuery({
    queryKey: ["dash", "courses"],
    queryFn: () => api<Page<unknown>>("/api/v1/courses?page=0&size=1"),
  });
  const invoices = useQuery({
    queryKey: ["dash", "invoices"],
    queryFn: () => api<Page<Invoice>>("/api/v1/invoices?page=0&size=100"),
  });

  const outstanding = (invoices.data?.items ?? [])
    .filter((i) => i.status === "SENT" || i.status === "OVERDUE")
    .reduce((sum, i) => sum + Number(i.amount), 0);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">
          Welcome back, {user?.fullName?.split(" ")[0] ?? "there"}
        </h1>
        <p className="text-sm text-slate-500">{tenant?.name} · signed in as {user?.role}</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {staff && (
          <StatCard label="Students" accent="text-indigo-600"
                    value={students.isLoading ? "…" : String(students.data?.totalItems ?? 0)}
                    hint="Active records" />
        )}
        <StatCard label="Courses" accent="text-fuchsia-600"
                  value={courses.isLoading ? "…" : String(courses.data?.totalItems ?? 0)}
                  hint="In catalogue" />
        <StatCard label={staff ? "Outstanding" : "You owe"} accent="text-amber-600"
                  value={invoices.isLoading ? "…" : `$${outstanding.toFixed(2)}`}
                  hint="Unpaid invoices" />
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="text-sm font-semibold text-slate-700">Getting started</h2>
        <ul className="mt-3 space-y-2 text-sm text-slate-600 list-disc list-inside">
          {staff && <li>Add students and create courses, then enroll students into them.</li>}
          {staff && <li>Record attendance per class session from the Attendance tab.</li>}
          <li>Review invoices under Billing{staff ? " and raise new ones" : ""}.</li>
          {hasRole("STUDENT") && <li>Pay an open invoice securely via Stripe Checkout.</li>}
        </ul>
      </div>
    </div>
  );
}
