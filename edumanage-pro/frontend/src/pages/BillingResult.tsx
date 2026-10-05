import { Link, useSearchParams } from "react-router-dom";

export default function BillingResult({ kind }: { kind: "success" | "cancel" }) {
  const [params] = useSearchParams();
  const invoice = params.get("invoice");
  const ok = kind === "success";

  return (
    <div className="mx-auto max-w-lg py-16 text-center">
      <div className={`mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full ${ok ? "bg-emerald-100" : "bg-amber-100"}`}>
        <svg className={`h-8 w-8 ${ok ? "text-emerald-600" : "text-amber-600"}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
          {ok
            ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />}
        </svg>
      </div>
      <h1 className="text-2xl font-bold text-slate-900">
        {ok ? "Payment received" : "Payment cancelled"}
      </h1>
      <p className="mt-2 text-slate-600">
        {ok
          ? "Thanks! Your payment went through on Stripe. The invoice is marked paid once the confirmation reaches our server."
          : "No charge was made. You can try again whenever you're ready."}
      </p>
      {invoice && <p className="mt-1 text-xs text-slate-400">Invoice ref: {invoice}</p>}
      <Link to="/invoices" className="mt-8 inline-block rounded-md bg-indigo-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-indigo-700">
        Back to Billing
      </Link>
    </div>
  );
}
