// Mirrors backend/src/main/java/com/cloudpulse/model enums exactly.
export type MonitorType = 'HTTP' | 'MOCK' | 'KEYWORD'

export type ServiceStatus = 'UNKNOWN' | 'HEALTHY' | 'DEGRADED' | 'DOWN' | 'RECOVERED'

export type EvaluationPolicy = 'NORMAL' | 'STRICT'

export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED'

export type Severity = 'INFO' | 'WARNING' | 'CRITICAL'

// Mirrors backend/src/main/java/com/cloudpulse/model/MonitoredService.java field-for-field
// (the entity is serialized directly by ServiceController, no DTO).
export interface MonitoredService {
  id: string
  name: string
  healthUrl: string
  serviceGroup: string | null
  tags: string[]
  monitorType: MonitorType
  currentStatus: ServiceStatus
  enabled: boolean
  checkIntervalSeconds: number
  activeEvaluationStrategy: EvaluationPolicy
  latencyThresholdMs: number | null
  availabilitySloPercent: number
  alertFailureThreshold: number
  consecutiveFailureCount: number
  createdAt: string
  lastCheckedAt: string | null
  expectedKeyword: string | null
}

export type CheckIntervalSeconds = 15 | 30 | 60 | 300

// Mirrors backend/src/main/java/com/cloudpulse/controller/RegisterServiceRequest.java
export interface RegisterServiceRequest {
  name: string
  healthUrl: string
  monitorType: MonitorType
  checkIntervalSeconds?: CheckIntervalSeconds
  expectedKeyword?: string
  serviceGroup: string | null
  tags: string[]
}

// Mirrors backend/src/main/java/com/cloudpulse/controller/AlertResponse.java
export interface AlertResponse {
  id: string
  serviceId: string | null
  serviceName: string | null
  severity: Severity
  status: AlertStatus
  message: string
  createdAt: string
  acknowledgedAt: string | null
  acknowledgedBy: string | null
  resolvedAt: string | null
}

export type IncidentActivityType = 'ACKNOWLEDGED' | 'ASSIGNED' | 'NOTE' | 'RESOLVED'

export interface IncidentActivityResponse {
  id: string
  type: IncidentActivityType
  author: string
  detail: string
  createdAt: string
}

export interface IncidentResponse {
  alertId: string
  owner: string | null
  activity: IncidentActivityResponse[]
}

export type WebhookProvider = 'SLACK' | 'TEAMS' | 'DISCORD'

export interface WebhookTargetResponse {
  id: string
  provider: WebhookProvider
  url: string
  label: string | null
  enabled: boolean
  createdAt: string
}

export interface RegisterWebhookRequest {
  provider: WebhookProvider
  url: string
  label?: string
}

export interface NotificationDeliveryResponse {
  id: string
  provider: WebhookProvider
  targetUrl: string
  serviceName: string | null
  message: string
  success: boolean
  httpStatus: number
  error: string | null
  sentAt: string
}

// Mirrors backend/src/main/java/com/cloudpulse/controller/StatusEventResponse.java
export interface StatusEventResponse {
  id: string
  serviceId: string | null
  serviceName: string | null
  previousStatus: ServiceStatus
  newStatus: ServiceStatus
  reason: string
  occurredAt: string
}

// Mirrors backend/src/main/java/com/cloudpulse/controller/HealthCheckResultResponse.java
export interface HealthCheckResultResponse {
  id: string
  serviceId: string | null
  serviceName: string | null
  checkedAt: string
  reachable: boolean
  httpStatus: number
  responseTimeMs: number
  rawMessage: string | null
  evaluatedStatus: ServiceStatus
}

// Mirrors backend/src/main/java/com/cloudpulse/controller/EvaluationPolicyResponse.java
export interface EvaluationPolicyResponse {
  serviceId: string
  serviceName: string
  evaluationPolicy: EvaluationPolicy
  currentStatus: ServiceStatus
}

export type MaintenanceWindowStatus = 'SCHEDULED' | 'ACTIVE' | 'ENDED'

// Mirrors backend/src/main/java/com/cloudpulse/controller/MaintenanceWindowResponse.java
export interface MaintenanceWindowResponse {
  id: string
  serviceId: string
  serviceName: string
  startsAt: string
  endsAt: string
  reason: string | null
  status: MaintenanceWindowStatus
}

export type SslCertificateStatus = 'NOT_APPLICABLE' | 'UNREACHABLE' | 'VALID' | 'EXPIRING_SOON' | 'EXPIRED'

// Mirrors backend/src/main/java/com/cloudpulse/controller/SslCertificateResponse.java
export interface SslCertificateResponse {
  serviceId: string
  applicable: boolean
  reachable: boolean
  expiresAt: string | null
  daysRemaining: number | null
  subject: string | null
  message: string
  status: SslCertificateStatus
}

// Mirrors backend/src/main/java/com/cloudpulse/controller/ReliabilityPolicyResponse.java
export interface ReliabilityPolicyResponse {
  serviceId: string
  serviceName: string
  evaluationPolicy: EvaluationPolicy
  latencyThresholdMs: number | null
  effectiveLatencyThresholdMs: number
  alertFailureThreshold: number
  consecutiveFailureCount: number
}

export type MetricsWindow = '1h' | '6h' | '24h' | '7d'

// Mirrors backend/src/main/java/com/cloudpulse/controller/ServiceMetricsResponse.java
export interface ServiceMetricsResponse {
  serviceId: string
  serviceName: string
  window: MetricsWindow
  windowStart: string
  windowEnd: string
  currentStatus: ServiceStatus
  enabled: boolean
  activeEvaluationStrategy: EvaluationPolicy
  totalChecks: number
  successfulChecks: number
  failedChecks: number
  availabilityPercent: number
  availabilitySloPercent: number
  errorBudgetRemainingPercent: number
  sloMet: boolean
  averageResponseTimeMs: number | null
  p95ResponseTimeMs: number | null
  p95SampleSize: number
  p95Sampled: boolean
}

export type HttpResponseFormat = 'STANDARD_JSON' | 'LEGACY_JSON' | 'GENERIC_HTTP'

// Mirrors backend/src/main/java/com/cloudpulse/controller/ProbeServiceResponse.java
export interface ProbeServiceResponse {
  reachable: boolean
  httpStatus: number
  responseTimeMs: number
  detectedMode: HttpResponseFormat | null
  finalUrl: string
  redirectCount: number
  responseBodyTruncated: boolean
  message: string
}

export type PublicOverallStatus = 'OPERATIONAL' | 'DEGRADED_PERFORMANCE' | 'MAJOR_OUTAGE' | 'CHECKING' | 'NO_SERVICES'

export interface PublicServiceStatusResponse {
  name: string
  serviceGroup: string | null
  status: ServiceStatus
  lastCheckedAt: string | null
}

export interface PublicStatusResponse {
  generatedAt: string
  overallStatus: PublicOverallStatus
  services: PublicServiceStatusResponse[]
}
