import {
  Area,
  AreaChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { HealthCheckResultResponse } from '../types'
import { formatDateTime, formatLatency } from '../lib/format'

export function ResponseTimeChart({
  results,
  p95,
}: {
  results: HealthCheckResultResponse[]
  p95: number | null
}) {
  const data = [...results]
    .reverse()
    .map((result) => ({
      id: result.id,
      timestamp: new Date(result.checkedAt).getTime(),
      time: new Intl.DateTimeFormat(undefined, {
        hour: 'numeric',
        minute: '2-digit',
      }).format(new Date(result.checkedAt)),
      fullTime: formatDateTime(result.checkedAt),
      responseTime: result.reachable ? result.responseTimeMs : null,
      status: result.evaluatedStatus,
    }))

  if (data.length < 2) {
    return (
      <div className="chart-empty" role="status">
        <span className="chart-empty__line" />
        <p>Two checks are needed to draw a response-time trend.</p>
      </div>
    )
  }

  return (
    <div className="h-[280px] w-full" aria-label="Response-time trend chart">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 16, right: 8, left: -18, bottom: 0 }} accessibilityLayer>
          <defs>
            <linearGradient id="responseTimeFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#2f6bff" stopOpacity={0.28} />
              <stop offset="90%" stopColor="#2f6bff" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke="#dce4ef" strokeDasharray="3 5" vertical={false} />
          <XAxis
            dataKey="time"
            axisLine={false}
            tickLine={false}
            minTickGap={36}
            tick={{ fill: '#6b7a90', fontSize: 11 }}
          />
          <YAxis
            axisLine={false}
            tickLine={false}
            width={58}
            tickFormatter={(value: number) => formatLatency(value)}
            tick={{ fill: '#6b7a90', fontSize: 11 }}
          />
          <Tooltip
            cursor={{ stroke: '#8fa4c1', strokeDasharray: '3 3' }}
            contentStyle={{
              borderRadius: 12,
              border: '1px solid #dce4ef',
              boxShadow: '0 12px 30px rgba(20, 33, 61, 0.12)',
              fontSize: 12,
            }}
            labelFormatter={(_, payload) => payload[0]?.payload.fullTime ?? ''}
            formatter={(value) => [formatLatency(typeof value === 'number' ? value : null), 'Response time']}
          />
          {p95 !== null && (
            <ReferenceLine
              y={p95}
              stroke="#e9a23b"
              strokeDasharray="5 5"
              label={{ value: `p95 ${formatLatency(p95)}`, fill: '#9a6517', fontSize: 11, position: 'insideTopRight' }}
            />
          )}
          <Area
            type="monotone"
            dataKey="responseTime"
            stroke="#2f6bff"
            strokeWidth={2.5}
            fill="url(#responseTimeFill)"
            connectNulls={false}
            activeDot={{ r: 5, fill: '#ffffff', stroke: '#2f6bff', strokeWidth: 3 }}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
