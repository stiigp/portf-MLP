interface PhaseNavigationButtonProps {
  disabled?: boolean
  label: string
  onClick: () => void
}

export function PhaseNavigationButton({
  disabled = false,
  label,
  onClick,
}: PhaseNavigationButtonProps) {
  return (
    <button
      className="phase-navigation-button"
      type="button"
      disabled={disabled}
      onClick={onClick}
    >
      {label}
    </button>
  )
}
