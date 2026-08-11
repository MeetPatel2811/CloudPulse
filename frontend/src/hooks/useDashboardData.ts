import { useCallback, useEffect, useRef, useState } from 'react'
import { getAlerts } from '../api/alerts'
import { getServices } from '../api/services'
import type { AlertResponse, MonitoredService } from '../types'

const REFRESH_INTERVAL_MS = 15_000

export function useDashboardData() {
  const [services, setServices] = useState<MonitoredService[] | null>(null)
  const [alerts, setAlerts] = useState<AlertResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)
  const mounted = useRef(true)

  const refresh = useCallback(async (showProgress = true) => {
    if (showProgress && mounted.current) setRefreshing(true)
    try {
      const [nextServices, nextAlerts] = await Promise.all([getServices(), getAlerts()])
      if (!mounted.current) return
      setServices(nextServices)
      setAlerts(nextAlerts)
      setError(null)
      setLastUpdated(new Date())
    } catch (caught: unknown) {
      if (!mounted.current) return
      setError(caught instanceof Error ? caught.message : 'CloudPulse could not refresh')
    } finally {
      if (mounted.current) setRefreshing(false)
    }
  }, [])

  useEffect(() => {
    mounted.current = true
    void refresh(false)

    const interval = window.setInterval(() => {
      if (document.visibilityState === 'visible') void refresh(false)
    }, REFRESH_INTERVAL_MS)

    const refreshWhenVisible = () => {
      if (document.visibilityState === 'visible') void refresh(false)
    }
    document.addEventListener('visibilitychange', refreshWhenVisible)

    return () => {
      mounted.current = false
      window.clearInterval(interval)
      document.removeEventListener('visibilitychange', refreshWhenVisible)
    }
  }, [refresh])

  return {
    services,
    alerts,
    error,
    refreshing,
    lastUpdated,
    refresh,
  }
}
