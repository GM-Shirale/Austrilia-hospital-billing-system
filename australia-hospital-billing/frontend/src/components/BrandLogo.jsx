import { Activity } from 'lucide-react';

/**
 * AustraCare brand: gradient pulse icon + wordmark + "MBS PRO" badge.
 * size: 'sm' (sidebar) | 'lg' (login page)
 */
export default function BrandLogo({ size = 'sm', subtitle = 'Hospital Billing & Clinical Engine' }) {
  const large = size === 'lg';
  return (
    <div className={`flex items-center ${large ? 'flex-col gap-3 text-center' : 'gap-3'}`}>
      <LogoMark size={large ? 64 : 44} />
      <div className="min-w-0">
        <div className={`flex items-center gap-2 ${large ? 'justify-center' : ''}`}>
          <span className={`font-black tracking-tight ${large ? 'text-3xl' : 'text-base'}`}>
            <span className="text-slate-900">AUSTRA</span>
            <span className="text-blue-600">CARE</span>
          </span>
          <span className="whitespace-nowrap rounded-md border border-blue-200 bg-blue-50 px-1.5 py-0.5 text-[10px] font-extrabold tracking-wider text-blue-700">
            MBS PRO
          </span>
        </div>
        {subtitle && <p className={`truncate text-slate-500 ${large ? 'text-sm' : 'text-xs'}`}>{subtitle}</p>}
      </div>
    </div>
  );
}

export function LogoMark({ size = 44 }) {
  return (
    <div className="relative shrink-0" style={{ width: size, height: size }}>
      <div
        className="flex h-full w-full items-center justify-center rounded-2xl bg-gradient-to-br from-blue-600 via-indigo-600 to-violet-600 text-white shadow-lg shadow-indigo-500/30"
      >
        <Activity style={{ width: size * 0.5, height: size * 0.5 }} strokeWidth={2.5} />
      </div>
      <span
        className="absolute -right-0.5 -top-0.5 rounded-full border-2 border-white bg-cyan-400"
        style={{ width: size * 0.26, height: size * 0.26 }}
      />
    </div>
  );
}
