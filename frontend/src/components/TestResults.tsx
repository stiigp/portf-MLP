import type { CSSProperties } from 'react'

export interface TestMetrics {
  accuracy: number
  precision: number
  recall: number
  f1Score: number
}

interface TestResultsProps {
  metrics: TestMetrics | null
}

const metricDefinitions: Array<{
  key: keyof TestMetrics
  label: string
}> = [
  { key: 'accuracy', label: 'Accuracy' },
  { key: 'precision', label: 'Precision' },
  { key: 'recall', label: 'Recall' },
  { key: 'f1Score', label: 'F1 score' },
]

export function TestResults({ metrics }: TestResultsProps) {
  return (
    <aside className="test-results" aria-labelledby="test-results-title">
      <h2 id="test-results-title">Test results</h2>
      <section className="test-result-metrics" aria-live="polite">
        {metricDefinitions.map((metric) => {
          const value = metrics?.[metric.key]

          return (
            <div
              className="test-result-metric"
              key={metric.key}
              style={getMetricStyle(value)}
            >
              <span>{metric.label}</span>
              <strong>{value == null ? 'N/A' : formatPercentage(value)}</strong>
            </div>
          )
        })}
      </section>
    </aside>
  )
}

function getMetricStyle(value: number | undefined): CSSProperties | undefined {
  if (value == null) {
    return undefined
  }

  return {
    '--test-result-color': getMetricColor(value),
  } as CSSProperties
}

function getMetricColor(value: number): string {
  if (value < 0.8) {
    return `hsl(${Math.round(355 + value * 6)} 74% 56%)`
  }

  if (value < 0.93) {
    return `hsl(${Math.round(28 + (value - 0.8) * 30)} 82% 54%)`
  }

  return `hsl(${Math.round(136 + (value - 0.93) * 95)} 62% 46%)`
}

function formatPercentage(value: number): string {
  return `${(value * 100).toFixed(1)}%`
}
