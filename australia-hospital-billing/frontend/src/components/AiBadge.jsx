import { Sparkles } from 'lucide-react';

/** Shows whether an answer came from the language model or from the rule-based fallback. */
export default function AiBadge({ source, model }) {
  const ai = source === 'AI';
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-semibold ring-1 ring-inset ${
        ai ? 'bg-violet-50 text-violet-700 ring-violet-200' : 'bg-slate-100 text-slate-600 ring-slate-200'
      }`}
      title={model}
    >
      <Sparkles className="h-3 w-3" />
      {ai ? `AI - ${model}` : 'Rule-based'}
    </span>
  );
}
