import type { SVGProps } from 'react'

export type IconName =
  | 'overview'
  | 'services'
  | 'incidents'
  | 'plus'
  | 'refresh'
  | 'arrow-left'
  | 'arrow-right'
  | 'check'
  | 'globe'
  | 'pulse'
  | 'clock'
  | 'search'
  | 'external'
  | 'close'
  | 'shield'
  | 'menu'
  | 'reports'
  | 'download'

const paths: Record<IconName, React.ReactNode> = {
  overview: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
  services: <><path d="M4 6h16M4 12h16M4 18h16"/><circle cx="7" cy="6" r="1" fill="currentColor" stroke="none"/><circle cx="7" cy="12" r="1" fill="currentColor" stroke="none"/><circle cx="7" cy="18" r="1" fill="currentColor" stroke="none"/></>,
  incidents: <><path d="M12 3 2.8 19a1.3 1.3 0 0 0 1.1 2h16.2a1.3 1.3 0 0 0 1.1-2L12 3Z"/><path d="M12 9v4"/><path d="M12 17h.01"/></>,
  plus: <path d="M12 5v14M5 12h14"/>,
  refresh: <><path d="M20 11a8 8 0 1 0-2.3 5.7"/><path d="M20 4v7h-7"/></>,
  'arrow-left': <><path d="m15 18-6-6 6-6"/><path d="M9 12h11"/></>,
  'arrow-right': <><path d="m9 18 6-6-6-6"/><path d="M4 12h11"/></>,
  check: <path d="m5 12 4 4L19 6"/>,
  globe: <><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/></>,
  pulse: <path d="M3 12h4l2-6 4 12 2-6h6"/>,
  clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
  search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
  external: <><path d="M14 4h6v6M20 4l-9 9"/><path d="M18 13v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h5"/></>,
  close: <path d="m6 6 12 12M18 6 6 18"/>,
  shield: <><path d="M12 3 5 6v5c0 4.6 2.8 8.1 7 10 4.2-1.9 7-5.4 7-10V6l-7-3Z"/><path d="m9 12 2 2 4-5"/></>,
  menu: <path d="M4 7h16M4 12h16M4 17h16"/>,
  reports: <><path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/><path d="m4 8 6-5 6 8 5-5"/></>,
  download: <><path d="M12 3v12m0 0 5-5m-5 5-5"/><path d="M5 21h14"/></>,
}

export function Icon({ name, ...props }: { name: IconName } & SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {paths[name]}
    </svg>
  )
}
