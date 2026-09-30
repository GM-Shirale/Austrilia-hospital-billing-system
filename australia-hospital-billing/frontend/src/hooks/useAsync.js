import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs an async loader and tracks { data, loading, error }. `reload()` re-runs it.
 * Ignores results of stale calls (e.g. when the user types quickly in a search box).
 */
export default function useAsync(loader, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const callId = useRef(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(loader, deps);

  const reload = useCallback(async () => {
    const id = ++callId.current;
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const data = await run();
      if (id === callId.current) setState({ data, loading: false, error: null });
    } catch (error) {
      if (id === callId.current) setState({ data: null, loading: false, error });
    }
  }, [run]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { ...state, reload, setData: (data) => setState((s) => ({ ...s, data })) };
}
