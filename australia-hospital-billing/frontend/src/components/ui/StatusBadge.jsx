import { label } from '../../utils/format';

const COLOURS = {
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  blue: 'bg-blue-50 text-blue-700 ring-blue-200',
  amber: 'bg-amber-50 text-amber-700 ring-amber-200',
  red: 'bg-rose-50 text-rose-700 ring-rose-200',
  slate: 'bg-slate-100 text-slate-600 ring-slate-200',
  violet: 'bg-violet-50 text-violet-700 ring-violet-200',
};

const STATUS_COLOUR = {
  ACTIVE: 'green', AVAILABLE: 'green', PAID: 'green', RECONCILED: 'green', COMPLETED: 'green', DISPENSED: 'green', ACCEPTED: 'green',
  ADMITTED: 'blue', FINALIZED: 'blue', SUBMITTED: 'blue', ACKNOWLEDGED: 'blue', VALIDATED: 'blue', COLLECTED: 'blue', OCCUPIED: 'blue',
  DRAFT: 'slate', DISCHARGED: 'slate', CLOSED: 'slate', INACTIVE: 'slate', ORDERED: 'slate', PRESCRIBED: 'slate',
  PARTIALLY_PAID: 'amber', SUBMISSION_QUEUED: 'violet', UNDER_MAINTENANCE: 'amber', SUSPENDED: 'amber', PARTIALLY_REFUNDED: 'amber',
  FULL: 'green', PARTIAL: 'amber', PENDING: 'red', IPD: 'blue', OPD: 'violet',
  REJECTED: 'red', CANCELLED: 'red', LAPSED: 'red', REFUNDED: 'red', DECEASED: 'red',
};

const TEXT = { IPD: 'IPD', OPD: 'OPD', FULL: 'Paid in full', PARTIAL: 'Part paid', PENDING: 'Payment pending' };

export default function StatusBadge({ status }) {
  const colour = COLOURS[STATUS_COLOUR[status] || 'slate'];
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${colour}`}>
      {TEXT[status] ?? label(status)}
    </span>
  );
}
