const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''

export function apiUrl(path: string): string {
  return `${apiBaseUrl}${path}`
}

export function apiWebSocketUrl(path: string): string {
  if (!apiBaseUrl) {
    return `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}${path}`
  }

  const url = new URL(apiBaseUrl)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  url.pathname = path
  url.search = ''
  url.hash = ''

  return url.toString()
}
