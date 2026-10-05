import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth, type RegisterPayload } from '../lib/auth';
import { ApiError } from '../lib/api';

const empty: RegisterPayload = {
  schoolName: '',
  slug: '',
  adminFullName: '',
  adminEmail: '',
  adminPassword: '',
};

export default function Register() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [form, setForm] = useState<RegisterPayload>(empty);
  const [slugEdited, setSlugEdited] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const slugify = (s: string) =>
    s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '');

  const set = (k: keyof RegisterPayload) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  // Auto-suggest a slug from the school name until the user edits the slug directly.
  const onSchoolName = (e: React.ChangeEvent<HTMLInputElement>) => {
    const schoolName = e.target.value;
    setForm((f) => ({ ...f, schoolName, slug: slugEdited ? f.slug : slugify(schoolName) }));
  };

  const onSlug = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSlugEdited(true);
    setForm((f) => ({ ...f, slug: e.target.value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);
    try {
      await register({ ...form, slug: form.slug.trim(), adminEmail: form.adminEmail.trim() });
      navigate('/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Registration failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const field = 'w-full px-5 py-3 rounded-xl bg-slate-900/50 border border-slate-700 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition-all';

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-[#0f172a] relative overflow-hidden font-sans py-10">
      <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-indigo-600 rounded-full mix-blend-screen filter blur-[120px] opacity-40 animate-pulse"></div>
      <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-fuchsia-600 rounded-full mix-blend-screen filter blur-[120px] opacity-40 animate-pulse" style={{ animationDelay: '2s' }}></div>

      <div className="w-full max-w-md p-8 relative z-10">
        <div className="backdrop-blur-xl bg-white/5 border border-white/10 p-10 rounded-3xl shadow-2xl">
          <div className="text-center mb-8">
            <h2 className="text-3xl font-bold text-white tracking-tight">Create your school</h2>
            <p className="text-slate-400 mt-3 text-sm">Set up EduManage Pro and your admin account.</p>
          </div>

          {error && (
            <div className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="text-sm font-medium text-slate-300 block ml-1 mb-1">School name</label>
              <input required value={form.schoolName} onChange={onSchoolName} className={field} placeholder="Springfield Academy" />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-300 block ml-1 mb-1">School ID (slug)</label>
              <input required value={form.slug} onChange={onSlug} pattern="[a-z0-9][a-z0-9-]{1,78}[a-z0-9]"
                     title="3–80 chars, lowercase letters, numbers and hyphens" className={field} placeholder="springfield-academy" />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-300 block ml-1 mb-1">Your name</label>
              <input required value={form.adminFullName} onChange={set('adminFullName')} className={field} placeholder="Jane Principal" />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-300 block ml-1 mb-1">Admin email</label>
              <input required type="email" value={form.adminEmail} onChange={set('adminEmail')} className={field} placeholder="admin@school.edu" />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-300 block ml-1 mb-1">Password</label>
              <input required type="password" minLength={10} value={form.adminPassword} onChange={set('adminPassword')} className={field} placeholder="At least 10 characters" />
            </div>

            <button type="submit" disabled={isLoading}
                    className="w-full rounded-xl bg-gradient-to-r from-indigo-500 to-fuchsia-500 px-8 py-3.5 text-white font-semibold tracking-wide hover:opacity-90 transition disabled:opacity-60 mt-2">
              {isLoading ? 'Creating…' : 'Create school'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-400">
            Already registered?{' '}
            <Link to="/login" className="font-semibold text-white hover:text-indigo-400 transition-colors">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
