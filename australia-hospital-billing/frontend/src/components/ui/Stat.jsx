export default function Stat({ icon: Icon, label, value, tone = 'blue', onClick }) {
  const tones = {
    blue: 'bg-blue-50 text-blue-600',
    green: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    violet: 'bg-violet-50 text-violet-600',
    rose: 'bg-rose-50 text-rose-600',
  };
  return (
    <button type="button" onClick={onClick} className="card flex items-center gap-4 p-5 text-left transition hover:shadow-md">
      <div className={`rounded-xl p-3 ${tones[tone]}`}>
        <Icon className="h-6 w-6" />
      </div>
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
        <p className="mt-0.5 text-2xl font-bold text-slate-900">{value}</p>
      </div>
    </button>
  );
}
