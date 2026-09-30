import { AlertTriangle, Inbox, Loader2 } from 'lucide-react';
import { errorMessage } from '../../api/client';
import Button from './Button';

export function Spinner({ text = 'Loading...' }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-sm text-slate-500">
      <Loader2 className="h-5 w-5 animate-spin" /> {text}
    </div>
  );
}

export function ErrorState({ error, onRetry }) {
  return (
    <div className="flex flex-col items-center gap-3 py-12 text-center">
      <AlertTriangle className="h-8 w-8 text-rose-500" />
      <p className="max-w-lg text-sm text-slate-600">{errorMessage(error)}</p>
      {error?.correlationId && <p className="text-xs text-slate-400">Correlation id: {error.correlationId}</p>}
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}

export function EmptyState({ title = 'Nothing here yet', text }) {
  return (
    <div className="flex flex-col items-center gap-2 py-12 text-center">
      <Inbox className="h-8 w-8 text-slate-300" />
      <p className="text-sm font-semibold text-slate-600">{title}</p>
      {text && <p className="text-sm text-slate-400">{text}</p>}
    </div>
  );
}

/** Renders spinner / error / empty / children depending on an useAsync() state. */
export function AsyncContent({ state, isEmpty, emptyTitle, emptyText, children }) {
  if (state.loading && !state.data) return <Spinner />;
  if (state.error) return <ErrorState error={state.error} onRetry={state.reload} />;
  if (isEmpty) return <EmptyState title={emptyTitle} text={emptyText} />;
  return children;
}
