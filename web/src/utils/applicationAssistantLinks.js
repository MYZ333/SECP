const PATIENT = ['PATIENT']

/**
 * Stable application-assistant link keys. Model output never becomes a route
 * directly: only entries in this table can be rendered or navigated.
 */
export const APPLICATION_ROUTE_LINKS = Object.freeze({
  dashboard: { path: '/dashboard', roles: PATIENT },
  'health-overview': { path: '/health', roles: PATIENT },
  'health-profile': { path: '/health/profile', roles: PATIENT },
  'health-metric': { path: '/health/metric', roles: PATIENT },
  'health-medical': { path: '/health/medical', roles: PATIENT },
  'health-report': { path: '/health/report', roles: PATIENT },
  'health-timeline': { path: '/health/timeline', roles: PATIENT },
  'point-overview': { path: '/point', roles: PATIENT },
  'point-mall': { path: '/point/mall', roles: PATIENT },
  'point-record': { path: '/point/record', roles: PATIENT },
  doctors: { path: '/doctor', roles: PATIENT },
  'health-assistant': { path: '/consult', roles: PATIENT },
  'doctor-consult': { path: '/doctor-consult', roles: PATIENT },
  alerts: { path: '/alert', roles: PATIENT },
  account: { path: '/account', roles: [] },
})

const APPLICATION_ROUTE_LABELS = Object.freeze({
  '首页': 'dashboard',
  '健康档案': 'health-overview',
  '基本信息': 'health-profile',
  '体征数据': 'health-metric',
  '就诊记录': 'health-medical',
  '健康报告': 'health-report',
  '健康时间轴': 'health-timeline',
  '积分中心': 'point-overview',
  '积分商城': 'point-mall',
  '积分明细': 'point-record',
  '医生专家库': 'doctors',
  '健康助手': 'health-assistant',
  '医生咨询': 'doctor-consult',
  '健康预警': 'alerts',
  '账户管理': 'account',
})

const INLINE_TOKEN_PATTERN = /\[([^\]\r\n]+)]\(([^)\r\n]+)\)|\*\*([^*\r\n]+)\*\*/g
const APP_DESTINATION_PATTERN = /^app:([a-z0-9-]+)$/

export function escapeApplicationAssistantHtml(value) {
  return String(value ?? '').replace(/[&<>'"]/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[char])
}

export function resolveApplicationAssistantRoute(routeKey, userRoles = []) {
  const route = APPLICATION_ROUTE_LINKS[routeKey]
  if (!route) return null
  if (!route.roles.length) return route.path
  const grantedRoles = new Set(Array.isArray(userRoles) ? userRoles : [])
  return route.roles.some(role => grantedRoles.has(role)) ? route.path : null
}

/**
 * Escapes all model text and turns exact `[label](app:route-key)` tokens into
 * links. As a deterministic fallback, a known page name explicitly emphasized
 * with `**page name**` is linked too. Other destinations remain plain text.
 */
export function renderApplicationAssistantLinks(value, userRoles = []) {
  const source = String(value ?? '')
  let html = ''
  let cursor = 0

  for (const match of source.matchAll(INLINE_TOKEN_PATTERN)) {
    const [raw, markdownLabel, destination, boldLabel] = match
    const offset = match.index ?? 0
    html += escapeApplicationAssistantHtml(source.slice(cursor, offset))

    const isMarkdownLink = markdownLabel != null
    const label = isMarkdownLink ? markdownLabel : boldLabel
    const appDestination = isMarkdownLink ? destination.trim().match(APP_DESTINATION_PATTERN) : null
    const routeKey = isMarkdownLink
      ? appDestination?.[1]
      : APPLICATION_ROUTE_LABELS[String(label || '').trim()]
    const path = routeKey ? resolveApplicationAssistantRoute(routeKey, userRoles) : null
    const safeLabel = escapeApplicationAssistantHtml(label)
    const content = isMarkdownLink ? safeLabel : `<strong>${safeLabel}</strong>`

    if (path) {
      html += `<a class="app-route-link" href="${escapeApplicationAssistantHtml(path)}" data-app-route="${routeKey}">${content}</a>`
    } else {
      html += content
    }
    cursor = offset + raw.length
  }

  return html + escapeApplicationAssistantHtml(source.slice(cursor))
}
