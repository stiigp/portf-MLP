export interface TrainingEventOptions {
  progressSampleInterval?: number;
  outputSampleInterval?: number;
  weightsSampleInterval?: number;
  progressMinMillis?: number;
  weightsMinMillis?: number;
}

export interface StartTrainingPayload {
  sessionId: string;
  databaseName: string;
  hiddenLayersNumber: number;
  activationFunctionName: string;
  learningRate: number;
  stopError: number;
  maxEpochs: number;
  eventOptions?: TrainingEventOptions;
}
