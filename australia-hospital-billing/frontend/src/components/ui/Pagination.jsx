import { ChevronLeft, ChevronRight } from 'lucide-react';

/** Works with the backend PageResponse: { page, size, totalElements, totalPages, last }. */
export default function Pagination({ page, onChange }) {
  if (!page || page.totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between border-t border-slate-100 px-4 py-3 text-sm text-slate-500">
      <span>
        Page {page.page + 1} of {page.totalPages} - {page.totalElements} records
      </span>
      <div className="flex gap-1">
        <button
          type="button"
          className="rounded-lg border border-slate-200 p-1.5 disabled:opacity-40"
          disabled={page.page === 0}
          onClick={() => onChange(page.page - 1)}
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <button
          type="button"
          className="rounded-lg border border-slate-200 p-1.5 disabled:opacity-40"
          disabled={page.last}
          onClick={() => onChange(page.page + 1)}
          aria-label="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
