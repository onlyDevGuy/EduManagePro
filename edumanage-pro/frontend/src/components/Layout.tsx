import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../lib/auth";

const NAV = [
  { to: "/",          label: "Dashboard",  icon: "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6", roles: ["ADMIN", "TEACHER", "STUDENT"] },
  { to: "/students",  label: "Students",   icon: "M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z", roles: ["ADMIN", "TEACHER"] },
  { to: "/courses",   label: "Courses",    icon: "M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253", roles: ["ADMIN", "TEACHER", "STUDENT"] },
  { to: "/attendance",label: "Attendance", icon: "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4", roles: ["ADMIN", "TEACHER"] },
  { to: "/invoices",  label: "Billing",    icon: "M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2zM10 8.5a.5.5 0 11-1 0 .5.5 0 011 0zm5 5a.5.5 0 11-1 0 .5.5 0 011 0z", roles: ["ADMIN", "TEACHER", "STUDENT"] },
];

export function Layout() {
  const { user, tenant, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-50 flex overflow-hidden w-full">
      {/* Mobile sidebar backdrop */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 bg-slate-900/50 z-20 lg:hidden backdrop-blur-sm"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <div className={`fixed inset-y-0 left-0 z-30 w-64 bg-slate-900 text-white transform transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:inset-0 flex flex-col ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="flex items-center justify-between h-16 px-6 bg-slate-950/50 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="h-8 w-8 rounded-xl bg-gradient-to-br from-indigo-500 to-fuchsia-500 text-white grid place-items-center font-bold shadow-lg shadow-indigo-500/20">E</div>
            <div className="font-bold tracking-tight">EduManage <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-fuchsia-400">Pro</span></div>
          </div>
          <button onClick={() => setSidebarOpen(false)} className="lg:hidden text-slate-400 hover:text-white">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div className="px-6 py-4 border-b border-slate-800">
          <div className="text-xs uppercase tracking-wider text-slate-500 font-semibold mb-1">Current Tenant</div>
          <div className="text-sm font-medium">{tenant?.name || "Loading..."}</div>
        </div>

        <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-1">
          {NAV.filter(n => hasRole(...(n.roles as any))).map(n => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.to === "/"}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 ${
                  isActive
                    ? "bg-indigo-500/10 text-indigo-400 font-medium"
                    : "text-slate-400 hover:bg-slate-800 hover:text-white"
                }`
              }
            >
              <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={n.icon} />
              </svg>
              {n.label}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-slate-800 bg-slate-900">
          <div className="flex items-center gap-3 mb-4">
            <div className="h-10 w-10 rounded-full bg-slate-800 flex items-center justify-center text-slate-300 font-medium border border-slate-700">
              {user?.fullName?.charAt(0) || "U"}
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-medium text-white truncate">{user?.fullName}</div>
              <div className="text-xs text-indigo-400 truncate">{user?.role}</div>
            </div>
          </div>
          <button
            onClick={() => { logout(); navigate("/login"); }}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 text-sm text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors border border-slate-800 hover:border-slate-700"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
            Sign out
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Mobile Header */}
        <header className="lg:hidden bg-white h-16 border-b border-slate-200 flex items-center justify-between px-4 sticky top-0 z-10">
          <button onClick={() => setSidebarOpen(true)} className="text-slate-500 hover:text-slate-900">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" /></svg>
          </button>
          <div className="font-bold text-slate-900">EduManage <span className="text-indigo-600">Pro</span></div>
          <div className="w-6"></div> {/* Spacer for centering */}
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-y-auto p-4 md:p-6 lg:p-8 bg-slate-50/50">
          <div className="max-w-7xl mx-auto h-full flex flex-col">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}