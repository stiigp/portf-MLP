import { useCallback, useEffect, useRef, useState } from 'react'

export type ToastTone = 'danger' | 'success' | 'warning' | 'orange' | 'info'

export interface ToastNotice {
  id: string
  title: string
  message?: string
  tone: ToastTone
  autoDismissMs?: number | null
}

type ToastDraft = Omit<ToastNotice, 'id'> & {
  id?: string
}

type ToastView = ToastNotice & {
  closing: boolean
}

const toastFadeMs = 220
let nextToastId = 0

export function useToastStack() {
  const [toasts, setToasts] = useState<ToastView[]>([])
  const timersRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(
    new Map(),
  )

  const dismissToast = useCallback((id: string) => {
    const dismissTimer = timersRef.current.get(id)

    if (dismissTimer) {
      clearTimeout(dismissTimer)
      timersRef.current.delete(id)
    }

    setToasts((currentToasts) =>
      currentToasts.map((toast) =>
        toast.id === id ? { ...toast, closing: true } : toast,
      ),
    )

    window.setTimeout(() => {
      setToasts((currentToasts) =>
        currentToasts.filter((toast) => toast.id !== id),
      )
    }, toastFadeMs)
  }, [])

  const pushToast = useCallback(
    (toast: ToastDraft) => {
      const id = toast.id ?? `toast-${Date.now()}-${nextToastId++}`

      const previousTimer = timersRef.current.get(id)
      if (previousTimer) {
        clearTimeout(previousTimer)
        timersRef.current.delete(id)
      }

      setToasts((currentToasts) => [
        ...currentToasts.filter((currentToast) => currentToast.id !== id),
        {
          id,
          title: toast.title,
          message: toast.message,
          tone: toast.tone,
          autoDismissMs: toast.autoDismissMs,
          closing: false,
        },
      ])

      if (toast.autoDismissMs != null) {
        timersRef.current.set(
          id,
          setTimeout(() => {
            dismissToast(id)
          }, toast.autoDismissMs),
        )
      }

      return id
    },
    [dismissToast],
  )

  useEffect(() => {
    return () => {
      timersRef.current.forEach((timer) => clearTimeout(timer))
      timersRef.current.clear()
    }
  }, [])

  return {
    dismissToast,
    pushToast,
    toasts,
  }
}

interface ToastStackProps {
  toasts: ToastView[]
  onDismiss: (id: string) => void
}

export function ToastStack({ toasts, onDismiss }: ToastStackProps) {
  if (toasts.length === 0) {
    return null
  }

  return (
    <div className="toast-stack" aria-live="polite" aria-atomic="false">
      {toasts.map((toast) => (
        <article
          className="training-toast"
          data-closing={toast.closing}
          data-tone={toast.tone}
          key={toast.id}
          role={toast.tone === 'danger' ? 'alert' : 'status'}
        >
          <button
            className="toast-close"
            type="button"
            aria-label="Close notification"
            onClick={() => onDismiss(toast.id)}
          >
            x
          </button>
          <strong>{toast.title}</strong>
          {toast.message ? <p>{toast.message}</p> : null}
        </article>
      ))}
    </div>
  )
}
