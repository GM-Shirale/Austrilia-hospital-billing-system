/** Label + control + optional hint. Children is the actual input/select. */
export default function Field({ label, hint, required, className = '', children }) {
  return (
    <label className={`block ${className}`}>
      <span className="label">
        {label}
        {required && <span className="text-rose-500"> *</span>}
      </span>
      {children}
      {hint && <span className="mt-1 block text-xs text-slate-400">{hint}</span>}
    </label>
  );
}
