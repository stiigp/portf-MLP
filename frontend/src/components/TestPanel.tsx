export interface TrainingSessionSummary {
  databaseName?: string
  networkError?: string
  sessionId?: string
  status?: string
}

interface TestPanelProps {
  onStartTest: () => void
  starting?: boolean
  trainingSession?: TrainingSessionSummary
}

export function TestPanel({
  onStartTest,
  starting = false,
  trainingSession,
}: TestPanelProps) {
  const details = trainingSession
    ? [
        { label: 'Database', value: trainingSession.databaseName },
        { label: 'Training error', value: trainingSession.networkError },
        { label: 'Session', value: trainingSession.sessionId },
        { label: 'Status', value: trainingSession.status },
      ].filter((detail): detail is { label: string; value: string } =>
        Boolean(detail.value),
      )
    : []

  return (
    <aside className="test-panel" aria-labelledby="testing-title">
      <h1 id="testing-title">Testing panel</h1>

      {details.length > 0 && (
        <section className="test-session-details" aria-label="Training session details">
          {details.map((detail) => (
            <div key={detail.label}>
              <span>{detail.label}</span>
              <strong>{detail.value}</strong>
            </div>
          ))}
        </section>
      )}

      <div className="actions">
        <button type="button" disabled={starting} onClick={onStartTest}>
          {starting ? 'Starting test...' : 'Start test'}
        </button>
      </div>
    </aside>
  )
}
