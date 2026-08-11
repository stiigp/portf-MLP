import type { ConnectionSnapshot } from '../types/TrainingEvent'

const SVG_NS = 'http://www.w3.org/2000/svg'

export function createSvgElement<K extends keyof SVGElementTagNameMap>(
  tagName: K,
  className?: string,
): SVGElementTagNameMap[K] {
  const element = document.createElementNS(SVG_NS, tagName)

  if (className) {
    element.setAttribute('class', className)
  }

  return element
}

export function connectionKey(
  connection: Pick<ConnectionSnapshot, 'from' | 'to'>,
): string {
  return `${connection.from}->${connection.to}`
}

export function formatNumber(value: number, precision = 3): string {
  return Number.isFinite(value) ? value.toPrecision(precision) : String(value)
}

export function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}
