import { useEffect, useMemo, useRef, useState } from 'react'
import type { StompSubscription } from '@stomp/stompjs'
import './App.css'

import {
  TrainingForm,
  type TrainingFormState,
} from './components/TrainingForm'
import { MlpVisualization } from './components/MlpVisualization'
import { createTrainingSession } from './services/mlpSessionApi'
import { stompClient } from './services/mlpStompClient'
import type { StartTrainingPayload } from './types/StartTrainingPayload'
import type {
  ConnectionSnapshot,
  LayerTopology,
  OutputValueSnapshot,
  TrainingEvent,
  TrainingFinishedEvent,
  TrainingProgressEvent,
} from './types/TrainingEvent'

type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error'

type TrainingStats = Pick<
  TrainingProgressEvent | TrainingFinishedEvent,
  'epoch' | 'sampleIndex' | 'networkError'
>

const initialTopology: LayerTopology[] = []
const initialOutputs: OutputValueSnapshot[] = []
const initialWeights: ConnectionSnapshot[] = []

const progressEventToStats = (
  event: TrainingProgressEvent | TrainingFinishedEvent,
): TrainingStats => ({
  epoch: event.epoch,
  sampleIndex: event.sampleIndex,
  networkError: event.networkError,
})

const isTrainingStatus = (status: string): boolean => {
  return status === 'QUEUED' || status === 'RUNNING'
}

const defaultEventOptions: StartTrainingPayload['eventOptions'] = {
  progressSampleInterval: 10,
  outputSampleInterval: 100,
  weightsSampleInterval: 50,
  progressMinMillis: 100,
  weightsMinMillis: 250,
}

function App() {
  const [stats, setStats] = useState<TrainingStats | null>(null)
  const [topology, setTopology] = useState<LayerTopology[]>(initialTopology)
  const [outputs, setOutputs] = useState<OutputValueSnapshot[]>(initialOutputs)
  const [weights, setWeights] = useState<ConnectionSnapshot[]>(initialWeights)
  const [training, setTraining] = useState<boolean>(false)
  const [connectionState, setConnectionState] =
    useState<ConnectionState>('disconnected')
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null)
  const trainingSubscriptionRef = useRef<StompSubscription | null>(null)

  const topologySummary = useMemo(() => {
    if (topology.length === 0) {
      return 'Topology not received yet'
    }

    return topology
      .map((layer) =>
        layer.type === 'hidden'
          ? `${layer.type}(${layer.index + 1}): ${layer.perceptronIds.length}`
          : `${layer.type}: ${layer.perceptronIds.length}`,
      )
      .join(' | ')
  }, [topology])

  useEffect(() => {
    return () => {
      trainingSubscriptionRef.current?.unsubscribe()
      void stompClient.disconnect()
    }
  }, [])

  function handleTrainingEvent(event: TrainingEvent): void {
    switch (event.type) {
      case 'TRAINING_STARTED':
        setTopology(event.layers)
        setStats(null)
        setOutputs(initialOutputs)
        setWeights(initialWeights)
        setTraining(true)
        break
      case 'TRAINING_PROGRESS':
        setStats(progressEventToStats(event))
        setTraining(true)
        break
      case 'OUTPUT_VALUES':
        setOutputs(event.outputs)
        setTraining(true)
        break
      case 'WEIGHTS_UPDATE':
        setWeights(event.connections)
        setTraining(true)
        break
      case 'SESSION_STATUS':
        setTraining(isTrainingStatus(event.status))
        break
      case 'TRAINING_FINISHED':
        setStats(progressEventToStats(event))
        setTraining(false)
        break
    }
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

  async function handleStartTraining(
    trainingForm: TrainingFormState,
  ): Promise<void> {
    try {
      const session = await createTrainingSession()
      await ensureConnected()

      trainingSubscriptionRef.current?.unsubscribe()
      trainingSubscriptionRef.current = stompClient.subscribeToTrainingStatus(
        session.sessionId,
        handleTrainingEvent,
      )

      setStats(null)
      setTopology(initialTopology)
      setOutputs(initialOutputs)
      setWeights(initialWeights)
      setCurrentSessionId(session.sessionId)
      setTraining(true)
      stompClient.startTraining({
        sessionId: session.sessionId,
        ...trainingForm,
        eventOptions: defaultEventOptions,
      })
    } catch (error) {
      setTraining(false)
      setConnectionState('error')
      console.error(error)
    }
  }

  return (
    <main className="app-shell">
      <section className="training-layout">
        <aside className="controls-panel" aria-labelledby="training-title">
          <div className="heading-group">
            <p className="eyebrow">MLP from scratch + websocket monitor</p>
            <h1 id="training-title">Training panel</h1>
          </div>

          <TrainingForm
            connectionBusy={connectionState === 'connecting'}
            training={training}
            onStartTraining={(trainingForm) => {
              void handleStartTraining(trainingForm)
            }}
          />
        </aside>

        <div className="visualization-panel">
          <MlpVisualization
            key={currentSessionId ?? 'no-session'}
            topology={topology}
            connections={weights}
            outputs={outputs}
          />
        </div>

        <aside className="stats-panel" aria-label="Training statistics">
          <h2>Live stats</h2>
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
              <span>Epoch</span>
              <strong>{stats?.epoch ?? 'N/A'}</strong>
            </div>
            <div>
              <span>Error</span>
              <strong>
                {stats ? formatNumber(stats.networkError) : 'N/A'}
              </strong>
            </div>
            <div>
              <span>Topology</span>
              <strong>{topologySummary}</strong>
            </div>
          </section>
        </aside>
      </section>
    </main>
  )
}

function formatNumber(value: number): string {
  return Number.isFinite(value) ? value.toPrecision(6) : String(value)
}

export default App
