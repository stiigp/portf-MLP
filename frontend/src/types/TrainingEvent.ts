export type TrainingEventType =
  | 'TRAINING_STARTED'
  | 'TRAINING_PROGRESS'
  | 'OUTPUT_VALUES'
  | 'WEIGHTS_UPDATE'
  | 'TRAINING_FINISHED'

export interface TrainingMessageBase {
  type: TrainingEventType
  sessionId: string
  epoch: number
  sampleIndex: number
}

export interface LayerTopology {
  type: 'input' | 'hidden' | 'output'
  index: number
  perceptronIds: string[]
}

export interface TrainingStartedEvent extends TrainingMessageBase {
  type: 'TRAINING_STARTED'
  layers: LayerTopology[]
}

export interface TrainingProgressEvent extends TrainingMessageBase {
  type: 'TRAINING_PROGRESS'
  networkError: number
}

export interface OutputValueSnapshot {
  id: string
  net: number | null
  output: number
  expected: number
}

export interface OutputValuesEvent extends TrainingMessageBase {
  type: 'OUTPUT_VALUES'
  outputs: OutputValueSnapshot[]
}

export interface ConnectionSnapshot {
  from: string
  to: string
  weight: number
}

export interface WeightsUpdateEvent extends TrainingMessageBase {
  type: 'WEIGHTS_UPDATE'
  connections: ConnectionSnapshot[]
}

export interface TrainingFinishedEvent extends TrainingMessageBase {
  type: 'TRAINING_FINISHED'
  networkError: number
}

export type TrainingEvent =
  | TrainingStartedEvent
  | TrainingProgressEvent
  | OutputValuesEvent
  | WeightsUpdateEvent
  | TrainingFinishedEvent
