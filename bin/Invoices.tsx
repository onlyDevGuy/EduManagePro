import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

interface Invoice {
  id: string; number: string; studentId: string; description: string;
  amount: number; currency: string; status: string;
  issueDate: string; dueDate: string; paidAt: string | null;
}
interface Page<T> { items: T[]; totalPages: number; totalItems: number; hasNext: boolean; page: number; }

const STATUS_STYLES: Record<string, string> = {
  PAID:    "bg-emerald-50 text-emerald-700",
  SENT:    "bg-blue-50 text-blue-700",
  OVERDUE: "bg-amber-50 text-amber-700",
  VOID:    "bg-slate-100 text-slate-500",
  DRAFT:   "bg-slate-100 text-slate-600",
};

export default function Invoices() {
  const { hasRole } = useAuth();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["invoices"],
    queryFn: () => api<Page<Invoice>>("/api/v1/invoices?page=0&size=50"),
  });

  const checkout = useMutation({
    mutationFn: (id: string) =>
      api<{ checkoutUrl: string }>(`/api/v1/invoices/${id}/checkout-session`, { method: "POST" }),
    onSuccess: (res) => { window.location.href = res.checkoutUrl; },
    onError: (e: any) => alert(e?.message ?? "Could not start checkout"),
  });

  const totalOutstanding = (data?.items ?? [])
    .filter(i => i.status === "SENT" || i.status === "OVERDUE")
    .reduce((sum, i) => sum + Number(i.amount), 0);

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">Billing</h1>
        <p className="text-sm text-slate-500">
          {data?.totalItems ?? 0} invoices · outstanding{" "}
          <span className="font-medium text-slate-800">${totalOutstanding.toFixed(2)}</span>
        </p>
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-slate-600">
            <tr>
              <th className="px-4 py-2 text-left font-medium">Number</th>
              <th className="px-4 py-2 text-left font-medium">Description</th>
              <th className="px-4 py-2 text-right font-medium">Amount</th>
              <th className="px-4 py-2 text-left font-medium">Due</th>
              <th className="px-4 py-2 text-left font-medium">Status</th>
              <th className="px-4 py-2" />
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading && <tr><td colSpan={6} className="px-4 py-6 text-center text-slate-500">Loading…</td></tr>}
            {data?.items.map(inv => (
              <tr key={inv.id} className="hover:bg-slate-50">
                <td className="px-4 py-2 font-mono text-xs text-slate-700">{inv.number}</td>
                <td className="px-4 py-2 text-slate-700">{inv.description}</td>
                <td className="px-4 py-2 text-right tabular-nums text-slate-900">
                  {inv.currency} {Number(inv.amount).toFixed(2)}
                </td>
                <td className="px-4 py-2 text-slate-600">{inv.dueDate}</td>
                <td className="px-4 py-2">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[inv.status] ?? ""}`}>
                    {inv.status}
                  </span>
                </td>
                <td className="px-4 py-2 text-right">
                  {(inv.status === "SENT" || inv.status === "OVERDUE") && (
                    <button
                      onClick={() => checkout.mutate(inv.id)}
                      disabled={checkout.isPending}
                      className="rounded-md bg-indigo-600 px-3 py-1 text-xs text-white hover:bg-indigo-700 disabled:opacity-50"
                    >
                      Pay now
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!hasRole("ADMIN") && (
        <p className="text-xs text-slate-500">
          You are viewing your own invoices. Contact the school office for billing disputes.
        </p>
      )}
    </div>
  );
}