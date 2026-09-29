import { useEffect, useMemo, useRef, useState } from 'react'
import type { StompSubscription } from '@stomp/stompjs'
import './App.css'

import {
  TrainingForm,
  type TrainingFormState,
} from './components/TrainingForm'
import { MlpVisualization } from './components/MlpVisualization'
import { PhaseNavigationButton } from './components/PhaseNavigationButton'
import { ConfusionMatrix } from './components/ConfusionMatrix'
import { TestPanel } from './components/TestPanel'
import { ToastStack, useToastStack } from './components/ToastStack'
import { createTestSession, createTrainingSession } from './services/mlpSessionApi'
import { stompClient } from './services/mlpStompClient'
import type { StartTrainingPayload } from './types/StartTrainingPayload'
import type { TestEvent } from './types/TestEvent'
import type {
  ConnectionSnapshot,
  LayerTopology,
  OutputValueSnapshot,
  TrainingEvent,
  TrainingFinishedEvent,
  TrainingProgressEvent,
  TrainingSessionStatusEvent,
} from './types/TrainingEvent'

type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error'
type Phase = 'training' | 'testing'

type TrainingStats = Pick<
  TrainingProgressEvent | TrainingFinishedEvent,
  'epoch' | 'sampleIndex' | 'networkError'
>

const initialTopology: LayerTopology[] = []
const initialOutputs: OutputValueSnapshot[] = []
const initialWeights: ConnectionSnapshot[] = []
const initialClassLabels: string[] = []
const initialClassSampleTotals: number[] = []
const initialConfusionMatrix: number[][] = []

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

const toastDismissMs = 20000

const getPhaseFromPathname = (): Phase =>
  window.location.pathname === '/testing' ? 'testing' : 'training'

function App() {
  const [stats, setStats] = useState<TrainingStats | null>(null)
  const [topology, setTopology] = useState<LayerTopology[]>(initialTopology)
  const [outputs, setOutputs] = useState<OutputValueSnapshot[]>(initialOutputs)
  const [weights, setWeights] = useState<ConnectionSnapshot[]>(initialWeights)
  const [training, setTraining] = useState<boolean>(false)
  const [sessionStatus, setSessionStatus] =
    useState<TrainingSessionStatusEvent | null>(null)
  const [connectionState, setConnectionState] =
    useState<ConnectionState>('disconnected')
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null)
  const [lastTrainingForm, setLastTrainingForm] =
    useState<TrainingFormState | null>(null)
  const [phase, setPhase] = useState<Phase>(getPhaseFromPathname)
  const [testingAvailable, setTestingAvailable] = useState(false)
  const [testing, setTesting] = useState(false)
  const [testClassLabels, setTestClassLabels] = useState(initialClassLabels)
  const [testClassSampleTotals, setTestClassSampleTotals] = useState(
    initialClassSampleTotals,
  )
  const [confusionMatrix, setConfusionMatrix] = useState(
    initialConfusionMatrix,
  )
  const { dismissToast, pushToast, toasts } = useToastStack()
  const trainingSubscriptionRef = useRef<StompSubscription | null>(null)
  const testSubscriptionRef = useRef<StompSubscription | null>(null)
  const lastQueueToastKeyRef = useRef<string | null>(null)
  const lastFailureToastKeyRef = useRef<string | null>(null)

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
      testSubscriptionRef.current?.unsubscribe()
      void stompClient.disconnect()
    }
  }, [])

  useEffect(() => {
    const handlePopState = () => {
      setPhase(getPhaseFromPathname())
    }

    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])
  function handleTrainingEvent(event: TrainingEvent): void {
    switch (event.type) {
      case 'TRAINING_STARTED':
        setTopology(event.layers)
        setStats(null)
        setOutputs(initialOutputs)
        setWeights(initialWeights)
        setTraining(true)
        setTestingAvailable(false)
        pushToast({
          title: 'Training started',
          message: 'The session left the queue and started processing data.',
          tone: 'success',
          autoDismissMs: toastDismissMs,
        })
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
        setSessionStatus(event)
        setTraining(isTrainingStatus(event.status))
        if (event.status === 'FINISHED') {
          setTestingAvailable(true)
        } else if (isTrainingStatus(event.status)) {
          setTestingAvailable(false)
        }
        notifySessionStatus(event)
        break
      case 'TRAINING_FINISHED':
        setStats(progressEventToStats(event))
        setTraining(false)
        setTestingAvailable(true)
        pushToast({
          title: 'Training finished',
          message: `Final network error: ${formatNumber(event.networkError)}.`,
          tone: 'success',
          autoDismissMs: toastDismissMs,
        })
        break
    }
  }

  function handleTestEvent(event: TestEvent): void {
    switch (event.type) {
      case 'TEST_STARTED':
        setTestClassLabels(event.classLabels)
        setTestClassSampleTotals(event.classSampleTotals)
        setConfusionMatrix(createEmptyConfusionMatrix(event.classLabels.length))
        setTesting(true)
        pushToast({
          title: 'Test started',
          message: `Processing ${event.totalSamples} test samples.`,
          tone: 'success',
          autoDismissMs: toastDismissMs,
        })
        break
      case 'TEST_PROGRESS':
        setConfusionMatrix((matrix) =>
          incrementConfusionMatrixCell(
            matrix,
            event.expectedClassIndex,
            event.predictedClassIndex,
          ),
        )
        break
      case 'TEST_FINISHED':
        setTesting(false)
        pushToast({
          title: 'Test finished',
          message: `Accuracy: ${formatPercentage(event.accuracy)}.`,
          tone: 'success',
          autoDismissMs: toastDismissMs,
        })
        break
    }
  }

  function notifySessionStatus(event: TrainingSessionStatusEvent): void {
    if (event.status === 'QUEUED') {
      const queueKey = `${event.sessionId}:${event.queuePosition ?? 'unknown'}`

      if (lastQueueToastKeyRef.current === queueKey) {
        return
      }

      lastQueueToastKeyRef.current = queueKey
      pushToast({
        title:
          event.queuePosition == null
            ? 'Training queued'
            : `Training queued #${event.queuePosition}`,
        message:
          event.queuePosition == null
            ? 'The session entered the queue and is waiting to run.'
            : `Your session is in queue position ${event.queuePosition}.`,
        tone: 'warning',
        autoDismissMs: toastDismissMs,
      })
      return
    }

    if (event.status === 'FAILED' || event.status === 'REJECTED') {
      const failureKey = `${event.sessionId}:${event.status}:${
        event.failureReason ?? 'unknown'
      }`

      if (lastFailureToastKeyRef.current === failureKey) {
        return
      }

      lastFailureToastKeyRef.current = failureKey
      pushToast({
        title:
          event.status === 'FAILED'
            ? 'Training failed'
            : 'Training rejected',
        message:
          event.failureReason ??
          'The training session could not be completed.',
        tone: 'danger',
        autoDismissMs: toastDismissMs,
      })
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
      setLastTrainingForm(trainingForm)
      setSessionStatus(null)
      setTraining(true)
      setTestingAvailable(false)
      lastQueueToastKeyRef.current = null
      lastFailureToastKeyRef.current = null
      stompClient.startTraining({
        sessionId: session.sessionId,
        ...trainingForm,
        eventOptions: defaultEventOptions,
      })
    } catch (error) {
      setTraining(false)
      setConnectionState('error')
      pushToast({
        title: 'Failed to start training',
        message:
          error instanceof Error
            ? error.message
            : 'The training session could not be created or connected.',
        tone: 'danger',
        autoDismissMs: toastDismissMs,
      })
      console.error(error)
    }
  }

  async function handleStartTest(): Promise<void> {
    if (!currentSessionId || !testingAvailable) {
      pushToast({
        title: 'Test unavailable',
        message: 'Finish a training session before starting its test.',
        tone: 'warning',
        autoDismissMs: toastDismissMs,
      })
      return
    }

    try {
      setTesting(true)
      setTestClassLabels(initialClassLabels)
      setTestClassSampleTotals(initialClassSampleTotals)
      setConfusionMatrix(initialConfusionMatrix)

      const testSession = await createTestSession(currentSessionId)
      await ensureConnected()

      testSubscriptionRef.current?.unsubscribe()
      testSubscriptionRef.current = stompClient.subscribeToTestEvents(
        testSession.testSessionId,
        handleTestEvent,
      )
      stompClient.startTest(testSession.testSessionId)
    } catch (error) {
      setTesting(false)
      setConnectionState('error')
      pushToast({
        title: 'Failed to start test',
        message:
          error instanceof Error
            ? error.message
            : 'The test session could not be created or connected.',
        tone: 'danger',
        autoDismissMs: toastDismissMs,
      })
      console.error(error)
    }
  }

  return (
    <main className="app-shell">
      {phase === 'training' ? (
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
              <strong>{formatTrainingStatus(training, sessionStatus)}</strong>
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
          <PhaseNavigationButton
            disabled={!testingAvailable}
            label="Testing Phase"
            onClick={() => navigateToPhase('testing', setPhase)}
          />
        </aside>
        </section>
      ) : (
        <section className="testing-layout" aria-label="Testing phase">
          <TestPanel
            onStartTest={() => {
              void handleStartTest()
            }}
            starting={testing}
            trainingSession={
              currentSessionId
                ? {
                    databaseName: lastTrainingForm?.databaseName,
                    networkError: stats
                      ? formatNumber(stats.networkError)
                      : undefined,
                    sessionId: currentSessionId,
                    status: sessionStatus?.status.toLowerCase(),
                  }
                : undefined
            }
          />
          <ConfusionMatrix
            classLabels={testClassLabels}
            classSampleTotals={testClassSampleTotals}
            matrix={confusionMatrix}
          />
          <PhaseNavigationButton
            label="Training Phase"
            onClick={() => navigateToPhase('training', setPhase)}
          />
        </section>
      )}

      <ToastStack toasts={toasts} onDismiss={dismissToast} />
    </main>
  )
}

function navigateToPhase(
  phase: Phase,
  setPhase: (phase: Phase) => void,
): void {
  const pathname = phase === 'testing' ? '/testing' : '/'

  window.history.pushState(null, '', pathname)
  setPhase(phase)
}
function formatTrainingStatus(
  training: boolean,
  sessionStatus: TrainingSessionStatusEvent | null,
): string {
  if (sessionStatus?.status === 'QUEUED' && sessionStatus.queuePosition != null) {
    return `queued (#${sessionStatus.queuePosition})`
  }

  return sessionStatus?.status.toLowerCase() ?? (training ? 'running' : 'idle')
}

function formatNumber(value: number): string {
  return Number.isFinite(value) ? value.toPrecision(6) : String(value)
}

function createEmptyConfusionMatrix(classCount: number): number[][] {
  return Array.from({ length: classCount }, () => Array(classCount).fill(0))
}

function incrementConfusionMatrixCell(
  matrix: number[][],
  expectedClassIndex: number,
  predictedClassIndex: number,
): number[][] {
  if (
    expectedClassIndex < 0 ||
    predictedClassIndex < 0 ||
    expectedClassIndex >= matrix.length ||
    predictedClassIndex >= matrix.length
  ) {
    return matrix
  }

  return matrix.map((row, rowIndex) =>
    row.map((value, columnIndex) =>
      rowIndex === expectedClassIndex && columnIndex === predictedClassIndex
        ? value + 1
        : value,
    ),
  )
}

function formatPercentage(value: number): string {
  return `${(value * 100).toFixed(1)}%`
}

export default App
