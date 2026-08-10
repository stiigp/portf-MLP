import type { CreateTrainingSessionResponse } from '../types/TrainingSession'

export async function createTrainingSession(): Promise<CreateTrainingSessionResponse> {
  const response = await fetch('/api/mlp/sessions', {
    method: 'POST',
  })

  if (!response.ok) {
    throw new Error(`Failed to create training session: ${response.status}`)
  }

  return response.json() as Promise<CreateTrainingSessionResponse>
}
