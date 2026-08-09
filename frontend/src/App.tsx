import { useEffect, useMemo, useRef, useState } from 'react'
import type { StompSubscription } from '@stomp/stompjs'
import './App.css'

import { stompClient } from './services/mlpStompClient'
import type {
  ConnectionSnapshot,
  LayerTopology,
  OutputValueSnapshot,
  TrainingEvent,
  TrainingFinishedEvent,
  TrainingProgressEvent,
} from './types/TrainingEvent'

const TRAINING_SESSION_ID = '1'

type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error'

interface TrainingSnapshot {
  lastEvent: TrainingEvent | null
  progress: TrainingProgressEvent | TrainingFinishedEvent | null
  topology: LayerTopology[]
  outputs: OutputValueSnapshot[]
  weights: ConnectionSnapshot[]
}

const initialSnapshot: TrainingSnapshot = {
  lastEvent: null,
  progress: null,
  topology: [],
  outputs: [],
  weights: [],
}

function App() {
  const [snapshot, setSnapshot] = useState<TrainingSnapshot>(initialSnapshot)
  const [training, setTraining] = useState<boolean>(false)
  const [connectionState, setConnectionState] =
    useState<ConnectionState>('disconnected')
  const trainingSubscriptionRef = useRef<StompSubscription | null>(null)

  const topologySummary = useMemo(() => {
    if (snapshot.topology.length === 0) {
      return 'Topology not received yet'
    }

    return snapshot.topology
      .map((layer) => `${layer.type}[${layer.index}]: ${layer.perceptronIds.length}`)
      .join(' | ')
  }, [snapshot.topology])

  useEffect(() => {
    return () => {
      trainingSubscriptionRef.current?.unsubscribe()
      void stompClient.disconnect()
    }
  }, [])

  function handleTrainingEvent(event: TrainingEvent): void {
    setSnapshot((current) => {
      const next: TrainingSnapshot = {
        ...current,
        lastEvent: event,
      }

      switch (event.type) {
        case 'TRAINING_STARTED':
          next.topology = event.layers
          next.progress = null
          next.outputs = []
          next.weights = []
          break
        case 'TRAINING_PROGRESS':
          next.progress = event
          break
        case 'OUTPUT_VALUES':
          next.outputs = event.outputs
          break
        case 'WEIGHTS_UPDATE':
          next.weights = event.connections
          break
        case 'TRAINING_FINISHED':
          next.progress = event
          break
      }

      return next
    })

    setTraining(event.type !== 'TRAINING_FINISHED')
  }

  async function ensureConnected(): Promise<void> {
    if (stompClient.connected) {
      setConnectionState('connected')
      return
    }

    setConnectionState('connecting')
    await stompClient.connect()
    setConnectionState('connected')
  }

  async function handleConnect(): Promise<void> {
    try {
      await ensureConnected()
    } catch (error) {
      setConnectionState('error')
      console.error(error)
    }
  }

  async function handleStartMushroomsTraining(): Promise<void> {
    try {
      await ensureConnected()

      if (!trainingSubscriptionRef.current) {
        trainingSubscriptionRef.current = stompClient.subscribeToTrainingStatus(
          TRAINING_SESSION_ID,
          handleTrainingEvent,
        )
      }

      setSnapshot(initialSnapshot)
      setTraining(true)
      stompClient.startMushroomsTraining(TRAINING_SESSION_ID)
    } catch (error) {
      setTraining(false)
      setConnectionState('error')
      console.error(error)
    }
  }

  const currentProgress = snapshot.progress
  const lastEvent = snapshot.lastEvent

  return (
    <main className="app-shell">
      <section className="training-panel">
        <div className="heading-group">
          <p className="eyebrow">MLP websocket monitor</p>
          <h1>Training status</h1>
          <p>
            Start a mushrooms training run and watch the backend event stream
            update this page.
          </p>
        </div>

        <div className="actions">
          <button
            type="button"
            onClick={handleConnect}
            disabled={connectionState === 'connecting' || stompClient.connected}
          >
            {connectionState === 'connected' ? 'Connected' : 'Connect'}
          </button>
          <button
            type="button"
            onClick={handleStartMushroomsTraining}
            disabled={training || connectionState === 'connecting'}
          >
            {training ? 'Training...' : 'Start mushrooms training'}
          </button>
        </div>

        <section className="status-grid" aria-live="polite">
          <div>
            <span>Connection</span>
            <strong>{connectionState}</strong>
          </div>
          <div>
            <span>Training</span>
            <strong>{training ? 'running' : 'idle'}</strong>
          </div>
          <div>
            <span>Last event</span>
            <strong>{lastEvent?.type ?? 'none'}</strong>
          </div>
          <div>
            <span>Session</span>
            <strong>{lastEvent?.sessionId ?? TRAINING_SESSION_ID}</strong>
          </div>
        </section>

        <section className="reacting-text">
          <h2>Reacting text</h2>
          <p>
            {currentProgress
              ? `Reacting to ${lastEvent?.type ?? 'event'} | epoch ${currentProgress.epoch} | sample ${currentProgress.sampleIndex} | error ${formatNumber(currentProgress.networkError)}`
              : lastEvent
                ? `Reacting to ${lastEvent.type} | epoch ${lastEvent.epoch} | sample ${lastEvent.sampleIndex}`
                : 'Waiting for backend events.'}
          </p>
        </section>

        <section className="details">
          <div>
            <span>Topology</span>
            <p>{topologySummary}</p>
          </div>
          <div>
            <span>Output values</span>
            <p>{formatOutputs(snapshot.outputs)}</p>
          </div>
          <div>
            <span>Weight update</span>
            <p>{formatWeights(snapshot.weights)}</p>
          </div>
        </section>
      </section>
    </main>
  )
}

function formatNumber(value: number): string {
  return Number.isFinite(value) ? value.toPrecision(6) : String(value)
}

function formatOutputs(outputs: OutputValueSnapshot[]): string {
  if (outputs.length === 0) {
    return 'No output values received yet'
  }

  return outputs
    .map(
      (output) =>
        `${output.id}: ${formatNumber(output.output)} expected ${output.expected}`,
    )
    .join(' | ')
}

function formatWeights(weights: ConnectionSnapshot[]): string {
  if (weights.length === 0) {
    return 'No weight update received yet'
  }

  const visibleWeights = weights
    .slice(0, 8)
    .map(
      (connection) =>
        `${connection.from} -> ${connection.to}: ${formatNumber(connection.weight)}`,
    )
    .join(' | ')

  const remainingCount = weights.length - 8

  return remainingCount > 0
    ? `${visibleWeights} | +${remainingCount} more`
    : visibleWeights
}

export default App
