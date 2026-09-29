import type { CreateTrainingSessionResponse } from '../types/TrainingSession'
import type { CreateTestSessionResponse } from '../types/TestSession'
import { apiUrl } from '../config/apiConfig'

export async function createTrainingSession(): Promise<CreateTrainingSessionResponse> {
  const response = await fetch(apiUrl('/api/mlp/sessions/train'), {
    method: 'POST',
  })

  if (!response.ok) {
    throw new Error(`Failed to create training session: ${response.status}`)
  }

  return response.json() as Promise<CreateTrainingSessionResponse>
}

export async function createTestSession(
  trainingSessionId: string,
): Promise<CreateTestSessionResponse> {
  const response = await fetch(apiUrl('/api/mlp/sessions/test'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ trainingSessionId }),
  })

  if (!response.ok) {
    throw new Error(`Failed to create test session: ${response.status}`)
  }

  return response.json() as Promise<CreateTestSessionResponse>
}
