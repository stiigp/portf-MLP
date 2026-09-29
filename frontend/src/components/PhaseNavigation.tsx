export type Phase = 'training' | 'testing'

interface PhaseNavigationProps {
  currentPhase: Phase
  onPhaseChange: (phase: Phase) => void
}

const phases: Array<{ label: string; value: Phase }> = [
  { label: 'Training', value: 'training' },
  { label: 'Testing', value: 'testing' },
]

export function PhaseNavigation({
  currentPhase,
  onPhaseChange,
}: PhaseNavigationProps) {
  return (
    <nav className="phase-navigation" aria-label="Application phase">
      {phases.map((phase) => {
        const isCurrent = phase.value === currentPhase

        return (
          <button
            key={phase.value}
            type="button"
            aria-current={isCurrent ? 'page' : undefined}
            disabled={isCurrent}
            onClick={() => onPhaseChange(phase.value)}
          >
            <span>{phase.label}</span>
          </button>
        )
      })}
    </nav>
  )
}
