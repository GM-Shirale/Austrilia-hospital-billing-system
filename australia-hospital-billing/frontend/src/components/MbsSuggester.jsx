import { useState } from 'react';
import { Sparkles } from 'lucide-react';
import { aiApi } from '../api';
import useAction from '../hooks/useAction';
import Button from './ui/Button';
import AiBadge from './AiBadge';
import { money } from '../utils/format';

/** Clinical notes -> suggested MBS items (Spring AI). onPick(suggestion) fills the bill line form. */
export default function MbsSuggester({ onPick }) {
  const [notes, setNotes] = useState('');
  const [result, setResult] = useState(null);
  const { busy, run } = useAction();

  const suggest = async () => {
    const response = await run(() => aiApi.mbsSuggestions(notes));
    if (response) setResult(response);
  };

  return (
    <div className="rounded-xl border border-violet-200 bg-violet-50/40 p-3">
      <p className="flex items-center gap-1 text-sm font-semibold text-violet-800">
        <Sparkles className="h-4 w-4" /> Suggest MBS items from clinical notes
      </p>
      <textarea
        className="input mt-2"
        rows={3}
        placeholder="e.g. Laparoscopic cholecystectomy for symptomatic gallstones, specialist review post-op"
        value={notes}
        onChange={(e) => setNotes(e.target.value)}
      />
      <p className="mt-1 text-xs text-slate-500">Names, Medicare numbers, phones and dates are removed before anything is sent to the model.</p>
      <Button variant="secondary" className="mt-2" icon={Sparkles} loading={busy} disabled={notes.trim().length < 5} onClick={suggest}>
        Suggest
      </Button>
      {result && (
        <div className="mt-3 space-y-2">
          <AiBadge source={result.source} model={result.model} />
          {result.suggestions.length === 0 && <p className="text-sm text-slate-500">No matching MBS items found.</p>}
          {result.suggestions.map((s) => (
            <button
              key={`${s.mbsItemNo}-${s.description}`}
              type="button"
              onClick={() => onPick(s)}
              className="block w-full rounded-lg border border-slate-200 bg-white p-2 text-left text-sm hover:border-violet-300"
            >
              <b>MBS {s.mbsItemNo}</b> - {s.description} <span className="text-xs text-slate-500">({s.itemType}{s.indicativeScheduleFee ? `, ~${money(s.indicativeScheduleFee)}` : ''})</span>
              {s.rationale && <span className="block text-xs text-slate-500">{s.rationale}</span>}
            </button>
          ))}
          <p className="text-xs text-slate-400">{result.disclaimer}</p>
        </div>
      )}
    </div>
  );
}
