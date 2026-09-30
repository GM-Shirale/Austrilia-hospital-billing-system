const aud = new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' });

export const money = (value) => (value === null || value === undefined ? '-' : aud.format(Number(value)));

export const date = (value) =>
  value ? new Date(value).toLocaleDateString('en-AU', { day: '2-digit', month: 'short', year: 'numeric' }) : '-';

export const dateTime = (value) =>
  value
    ? new Date(value).toLocaleString('en-AU', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : '-';

/** "SUBMISSION_QUEUED" -> "Submission queued" */
export const label = (value) =>
  value ? value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ') : '-';

/** Value for <input type="datetime-local"> in local time. */
export const toLocalInput = (d = new Date()) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};
