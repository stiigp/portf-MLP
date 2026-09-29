export interface TestStartedEvent {
  type: 'TEST_STARTED'
  testSessionId: string
  trainingSessionId: string
  totalSamples: number
  classLabels: string[]
  classSampleTotals: number[]
}

export interface TestProgressEvent {
  type: 'TEST_PROGRESS'
  testSessionId: string
  sampleIndex: number
  predictedClassIndex: number
  expectedClassIndex: number
  accuracy: number
  precision: number
  f1Score: number
}

export interface TestFinishedEvent {
  type: 'TEST_FINISHED'
  testSessionId: string
  processedSamples: number
  correctPredictions: number
  accuracy: number
  precision: number
  f1Score: number
}

export type TestEvent =
  | TestStartedEvent
  | TestProgressEvent
  | TestFinishedEvent
