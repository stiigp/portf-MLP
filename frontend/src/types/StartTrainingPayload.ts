export interface StartTrainingPayload {
  sessionId: string;
  databaseName: string;
  hiddenLayersNumber: number;
  activationFunctionName: string;
  learningRate: number;
  stopError: number;
  maxEpochs: number;
}
