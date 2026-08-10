import type { TrainingSessionStatus } from './TrainingEvent'

export interface CreateTrainingSessionResponse {
  sessionId: string
  status: TrainingSessionStatus
}
