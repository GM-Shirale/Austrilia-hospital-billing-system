import { useState } from 'react';
import { FileDown } from 'lucide-react';
import toast from 'react-hot-toast';
import Button from './ui/Button';
import { billingApi } from '../api';
import { errorMessage } from '../api/client';

/** "Download Tax Invoice PDF": fetches the invoice data from the backend and renders the A4 PDF. */
export default function InvoiceButton({ billId, variant = 'primary', className = '' }) {
  const [busy, setBusy] = useState(false);
  const download = async () => {
    setBusy(true);
    try {
      const invoice = await billingApi.invoice(billId);
      const { downloadTaxInvoicePdf } = await import('../utils/pdf');
      await downloadTaxInvoicePdf(invoice);
      toast.success(`Tax invoice ${invoice.invoiceNo} downloaded`);
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };
  return (
    <Button variant={variant} icon={FileDown} loading={busy} onClick={download} className={className}>
      Download Tax Invoice PDF
    </Button>
  );
}
