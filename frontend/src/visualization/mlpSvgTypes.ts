import type {
  ConnectionSnapshot,
  LayerTopology,
  OutputValueSnapshot,
  TrainingEventType,
  TrainingFinishedEvent,
  TrainingProgressEvent,
  TrainingSessionStatus,
} from '../types/TrainingEvent'

export const VIEWBOX_WIDTH = 920
export const VIEWBOX_HEIGHT = 520

export type Point = {
  x: number
  y: number
}

export type LayerLayout = {
  layer: LayerTopology
  x: number
  y: number
  height: number
}

export type MlpLayoutState = {
  width: number
  height: number
  layers: LayerLayout[]
  neurons: Map<string, Point>
}

export type MlpSvgSnapshot = {
  topology: LayerTopology[]
  connections: ConnectionSnapshot[]
  outputs: OutputValueSnapshot[]
  progress: TrainingProgressEvent | TrainingFinishedEvent | null
  status: TrainingSessionStatus | null
  eventType: TrainingEventType | null
  selectedNeuronId?: string | null
  selectedConnectionKey?: string | null
}
