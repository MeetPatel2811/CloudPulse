import { useEffect, useMemo, useState } from 'react'
import { AddMonitorWizard } from './components/AddMonitorWizard'
import { DashboardOverview } from './components/DashboardOverview'
import { Icon, type IconName } from './components/Icon'
import { IncidentCenter } from './components/IncidentCenter'
import { ServiceDetailView } from './components/ServiceDetailView'
import { ServiceDirectory } from './components/ServiceDirectory'
import { SloDashboard } from './components/SloDashboard'
import { PublicStatusPage } from './components/PublicStatusPage'
import { useDashboardData } from './hooks/useDashboardData'
import { useFleetInsights } from './hooks/useFleetInsights'
import type { MetricsWindow } from './types'
import { formatRefreshAge } from './lib/format'

type AppView = 'overview' | 'services' | 'incidents' | 'reports'

const navigation: { id: AppView; label: string; icon: IconName }[] = [
  { id: 'overview', label: 'Overview', icon: 'overview' },
  { id: 'services', label: 'Services', icon: 'services' },
  { id: 'incidents', label: 'Incidents', icon: 'incidents' },
  { id: 'reports', label: 'SLO reports', icon: 'reports' },
]

function OperatorApp() {
  const [view, setView] = useState<AppView>('overview')
  const [selectedServiceId, setSelectedServiceId] = useState<string | null>(null)
  const [addMonitorOpen, setAddMonitorOpen] = useState(false)
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const [currentTime, setCurrentTime] = useState(() => Date.now())
  const [fleetWindow, setFleetWindow] = useState<MetricsWindow>('24h')

  const {
    services,
    alerts,
    error,
    refreshing,
    lastUpdated,
    refresh,
  } = useDashboardData()
  const refreshVersion = lastUpdated?.getTime() ?? 0
  const refreshAgeSeconds = lastUpdated
    ? Math.max(0, Math.floor((currentTime - lastUpdated.getTime()) / 1_000))
    : null
  const refreshStatus = lastUpdated && refreshAgeSeconds !== null
    ? `${error ? 'Refresh delayed' : 'Live · refreshed'} ${formatRefreshAge(refreshAgeSeconds)}`
    : 'Connecting…'
  const { metrics, histories, loading: insightsLoading } = useFleetInsights(
    services,
    fleetWindow,
    refreshVersion,
  )

  const selectedService = useMemo(
    () => services?.find((service) => service.id === selectedServiceId) ?? null,
    [selectedServiceId, services],
  )
  const activeIncidentCount = alerts?.filter((alert) => alert.status !== 'RESOLVED').length ?? 0

  useEffect(() => {
    setCurrentTime(Date.now())
    const timer = window.setInterval(() => setCurrentTime(Date.now()), 1_000)
    return () => window.clearInterval(timer)
  }, [lastUpdated])

  function navigate(nextView: AppView) {
    setView(nextView)
    setSelectedServiceId(null)
    setMobileNavOpen(false)
  }

  function selectService(serviceId: string) {
    setSelectedServiceId(serviceId)
    setMobileNavOpen(false)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const pageTitle = selectedService
    ? selectedService.name
    : view === 'overview'
      ? 'Operations overview'
      : view === 'services'
        ? 'Services'
        : view === 'incidents'
          ? 'Incidents'
          : 'SLO reports'

  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">Skip to main content</a>

      <aside className={`sidebar ${mobileNavOpen ? 'sidebar--open' : ''}`}>
        <div className="brand-lockup">
          <div className="brand-mark" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <div>
            <strong>CloudPulse</strong>
            <span>Signal operations</span>
          </div>
        </div>

        <nav aria-label="Primary navigation">
          <p className="sidebar-label">Workspace</p>
          {navigation.map((item) => (
            <button
              type="button"
              key={item.id}
              className={view === item.id && !selectedServiceId ? 'nav-item nav-item--active' : 'nav-item'}
              aria-current={view === item.id && !selectedServiceId ? 'page' : undefined}
              onClick={() => navigate(item.id)}
            >
              <Icon name={item.icon} className="h-[18px] w-[18px]" />
              <span>{item.label}</span>
              {item.id === 'incidents' && activeIncidentCount > 0 && (
                <span className="nav-count">{activeIncidentCount}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-status">
          <div className="sidebar-status__pulse" aria-hidden="true"><span /></div>
          <div>
            <strong>Live monitoring</strong>
            <span>Refreshes every 15 seconds</span>
          </div>
        </div>

        <div className="sidebar-footer">
          <span className="operator-avatar" aria-hidden="true">AT</span>
          <div><strong>Operator</strong><span>CloudPulse team</span></div>
        </div>
      </aside>

      {mobileNavOpen && (
        <button
          type="button"
          className="mobile-nav-backdrop"
          aria-label="Close navigation"
          onClick={() => setMobileNavOpen(false)}
        />
      )}

      <div className="app-workspace">
        <header className="topbar">
          <div className="topbar__title">
            <button
              type="button"
              className="icon-button topbar__menu"
              onClick={() => setMobileNavOpen(true)}
              aria-label="Open navigation"
            >
              <Icon name="menu" className="h-5 w-5" />
            </button>
            <div>
              <span className="topbar__breadcrumb">CloudPulse / Production</span>
              <h1>{pageTitle}</h1>
            </div>
          </div>
          <div className="topbar__actions">
            <span
              className={`last-updated ${error ? 'last-updated--delayed' : ''}`}
              aria-label={refreshStatus}
              title={lastUpdated ? `Last successful refresh at ${lastUpdated.toLocaleTimeString()}` : undefined}
            >
              <span className={`live-dot ${error ? 'live-dot--delayed' : ''}`} aria-hidden="true" />
              {refreshStatus}
            </span>
            <button
              type="button"
              className="icon-button"
              onClick={() => void refresh(true)}
              disabled={refreshing}
              aria-label="Refresh dashboard"
              title="Refresh dashboard"
            >
              <Icon name="refresh" className={`h-[18px] w-[18px] ${refreshing ? 'spin' : ''}`} />
            </button>
            <button
              type="button"
              className="button button--primary"
              aria-label="Add monitor"
              onClick={() => setAddMonitorOpen(true)}
            >
              <Icon name="plus" className="h-4 w-4" />
              <span className="topbar__add-label">Add monitor</span>
            </button>
          </div>
        </header>

        <main id="main-content" className="main-content">
          {error && (
            <div className="connection-banner" role="alert">
              <span><Icon name="incidents" className="h-4 w-4" /> Live refresh failed: {error}</span>
              <button type="button" onClick={() => void refresh(true)}>Try again</button>
            </div>
          )}

          {services === null || alerts === null ? (
            <div className="dashboard-loading" role="status" aria-live="polite">
              <div className="dashboard-loading__pulse" aria-hidden="true"><span /></div>
              <h2>Reading the fleet</h2>
              <p>Loading services, incidents, and recent signals…</p>
            </div>
          ) : selectedService ? (
            <ServiceDetailView
              service={selectedService}
              alerts={alerts}
              refreshVersion={refreshVersion}
              onBack={() => setSelectedServiceId(null)}
              onChanged={() => refresh(false)}
            />
          ) : view === 'overview' ? (
            <DashboardOverview
              services={services}
              alerts={alerts}
              metrics={metrics}
              histories={histories}
              insightsLoading={insightsLoading}
              onSelectService={selectService}
              onAddMonitor={() => setAddMonitorOpen(true)}
              onShowIncidents={() => navigate('incidents')}
            />
          ) : view === 'services' ? (
            <ServiceDirectory
              services={services}
              metrics={metrics}
              histories={histories}
              onAddMonitor={() => setAddMonitorOpen(true)}
              onSelectService={selectService}
              onChanged={() => refresh(false)}
            />
          ) : view === 'incidents' ? (
            <IncidentCenter
              alerts={alerts}
              onChanged={() => refresh(false)}
              onSelectService={selectService}
            />
          ) : (
            <SloDashboard
              services={services}
              metrics={metrics}
              window={fleetWindow}
              loading={insightsLoading}
              onWindowChange={setFleetWindow}
              onSelectService={selectService}
            />
          )}
        </main>
      </div>

      {addMonitorOpen && (
        <AddMonitorWizard
          onClose={() => setAddMonitorOpen(false)}
          onCreated={async (service) => {
            await refresh(false)
            setSelectedServiceId(service.id)
          }}
        />
      )}
    </div>
  )
}

function App() {
  if (window.location.pathname === '/status' || window.location.pathname.startsWith('/status/')) {
    return <PublicStatusPage />
  }
  return <OperatorApp />
}

export default App
