import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import { patientApi } from '../api';
import useDebounce from '../hooks/useDebounce';

/** Type-ahead patient search; calls onSelect(patientSummary). */
export default function PatientPicker({ onSelect }) {
  const [term, setTerm] = useState('');
  const [results, setResults] = useState([]);
  const debounced = useDebounce(term);

  useEffect(() => {
    if (!debounced) {
      setResults([]);
      return;
    }
    patientApi.search(debounced, 0, 8).then((page) => setResults(page.content)).catch(() => setResults([]));
  }, [debounced]);

  return (
    <div className="relative">
      <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
      <input className="input pl-9" placeholder="Search name, MRN or Medicare no" value={term} onChange={(e) => setTerm(e.target.value)} />
      {results.length > 0 && (
        <ul className="absolute z-10 mt-1 w-full overflow-hidden rounded-lg border border-slate-200 bg-white shadow-lg">
          {results.map((p) => (
            <li key={p.id}>
              <button
                type="button"
                className="w-full px-3 py-2 text-left text-sm hover:bg-blue-50"
                onClick={() => {
                  onSelect(p);
                  setTerm('');
                  setResults([]);
                }}
              >
                <span className="font-semibold">{p.fullName}</span>
                <span className="ml-2 text-xs text-slate-500">
                  {p.mrn} {p.medicareNo ? `- Medicare ${p.medicareNo}` : ''}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
