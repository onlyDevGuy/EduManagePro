import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../lib/auth";

const NAV = [
  { to: "/",          label: "Dashboard",  roles: ["ADMIN", "TEACHER", "STUDENT"] },
  { to: "/students",  label: "Students",   roles: ["ADMIN", "TEACHER"] },
  { to: "/courses",   label: "Courses",    roles: ["ADMIN", "TEACHER", "STUDENT"] },
  { to: "/attendance",label: "Attendance", roles: ["ADMIN", "TEACHER"] },
  { to: "/invoices",  label: "Billing",    roles: ["ADMIN", "TEACHER", "STUDENT"] },
];

export function Layout() {
  const { user, tenant, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="bg-white border-b border-slate-200">
        <div className="mx-auto max-w-7xl px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-8 w-8 rounded-lg bg-indigo-600 text-white grid place-items-center font-bold">E</div>
            <div>
              <div className="font-semibold text-slate-900 leading-tight">EduManage Pro</div>
              <div className="text-xs text-slate-500 leading-tight">{tenant?.name}</div>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right hidden sm:block">
              <div className="text-sm font-medium text-slate-800">{user?.fullName}</div>
              <div className="text-xs text-slate-500">{user?.role}</div>
            </div>
            <button
              onClick={() => { logout(); navigate("/login"); }}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-100"
            >
              Sign out
            </button>
          </div>
        </div>
        <nav className="mx-auto max-w-7xl px-4 flex gap-1 overflow-x-auto">
          {NAV.filter(n => hasRole(...(n.roles as any))).map(n => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.to === "/"}
              className={({ isActive }) =>
                `px-3 py-2 text-sm border-b-2 -mb-px whitespace-nowrap ${
                  isActive
                    ? "border-indigo-600 text-indigo-700 font-medium"
                    : "border-transparent text-slate-600 hover:text-slate-900"
                }`
              }
            >
              {n.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}