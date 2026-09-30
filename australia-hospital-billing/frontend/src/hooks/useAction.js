import { useState } from 'react';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';

/** Wraps a mutation: shows a spinner flag, a success toast and a readable error toast. */
export default function useAction() {
  const [busy, setBusy] = useState(false);

  const run = async (action, successMessage) => {
    setBusy(true);
    try {
      const result = await action();
      if (successMessage) toast.success(successMessage);
      return result;
    } catch (err) {
      toast.error(errorMessage(err), { duration: 6000 });
      return undefined;
    } finally {
      setBusy(false);
    }
  };

  return { busy, run };
}
