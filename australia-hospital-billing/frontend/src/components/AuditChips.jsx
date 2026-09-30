import { UserCheck } from 'lucide-react';

const TONES = {
  slate: 'bg-slate-100 text-slate-600',
  blue: 'bg-blue-50 text-blue-700',
  green: 'bg-emerald-50 text-emerald-700',
  red: 'bg-rose-50 text-rose-700',
};

/** "Who did what" badges: chips = [{ text, tone }] (falsy entries are skipped). */
export function AuditChips({ chips }) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {chips.filter(Boolean).map((c) => (
        <span key={c.text} className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs ${TONES[c.tone || 'slate']}`}>
          <UserCheck className="h-3 w-3" />
          {c.text}
        </span>
      ))}
    </div>
  );
}

/** " (entered by X)" when someone other than the doctor keyed the order in, else "". */
export const enteredBy = (doctorName, userName) => {
  if (!userName) return '';
  const surname = (doctorName || '').replace(/^Dr\s+/, '').split(' ').pop();
  return surname && userName.includes(surname) ? '' : ` (entered by ${userName})`;
};
