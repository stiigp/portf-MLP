export type MlpStatus = Record<string, never>;

export interface ConnectionSnapshot {
  from: string;
  to: string;
  weight: number;
}

export interface InputSnapshot {
  fromPerceptronId: string;
  weight: number;
  inputOutput: number;
}

export interface PerceptronSnapshot {
  id: string;
  net: number | null;
  output: number | null;
  error: number | null;
  gradient: number | null;
  inputs: InputSnapshot[];
}

export interface LayerSnapshot {
  type: string;
  index: number;
  perceptrons: PerceptronSnapshot[];
}

export interface TrainingEvent {
  type: string;
  sessionId: string;
  epoch: number;
  sampleIndex: number;
  networkError: number;
  inputLayer: LayerSnapshot;
  hiddenLayers: LayerSnapshot[];
  outputLayer: LayerSnapshot;
}
