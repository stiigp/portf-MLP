export type TrainingEventType =
  | 'SESSION_STATUS'
  | 'TRAINING_STARTED'
  | 'TRAINING_PROGRESS'
  | 'OUTPUT_VALUES'
  | 'WEIGHTS_UPDATE'
  | 'TRAINING_FINISHED'

export interface TrainingMessageBase {
  type: TrainingEventType
  sessionId: string
}

export type TrainingSessionStatus =
  | 'CREATED'
  | 'QUEUED'
  | 'RUNNING'
  | 'FINISHED'
  | 'FAILED'
  | 'REJECTED'

export interface TrainingSampleEventBase extends TrainingMessageBase {
  epoch: number
  sampleIndex: number
}

export interface LayerTopology {
  type: 'input' | 'hidden' | 'output'
  index: number
  perceptronIds: string[]
}

export interface TrainingSessionStatusEvent extends TrainingMessageBase {
  type: 'SESSION_STATUS'
  status: TrainingSessionStatus
  failureReason: string | null
}

export interface TrainingStartedEvent extends TrainingSampleEventBase {
  type: 'TRAINING_STARTED'
  layers: LayerTopology[]
}

export interface TrainingProgressEvent extends TrainingSampleEventBase {
  type: 'TRAINING_PROGRESS'
  networkError: number
}

export interface OutputValueSnapshot {
  id: string
  net: number | null
  output: number
  expected: number
}

export interface OutputValuesEvent extends TrainingSampleEventBase {
  type: 'OUTPUT_VALUES'
  outputs: OutputValueSnapshot[]
}

export interface ConnectionSnapshot {
  from: string
  to: string
  weight: number
}

export interface WeightsUpdateEvent extends TrainingSampleEventBase {
  type: 'WEIGHTS_UPDATE'
  connections: ConnectionSnapshot[]
}

export interface TrainingFinishedEvent extends TrainingSampleEventBase {
  type: 'TRAINING_FINISHED'
  networkError: number
}

export type TrainingEvent =
  | TrainingSessionStatusEvent
  | TrainingStartedEvent
  | TrainingProgressEvent
  | OutputValuesEvent
  | WeightsUpdateEvent
  | TrainingFinishedEvent
