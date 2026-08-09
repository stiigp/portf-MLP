export interface TrainingFormState {
  hiddenLayersNumber: number
  activationFunctionName: string
  learningRate: number
  stopError: number
  maxEpochs: number
}

interface TrainingFormProps {
  connectionBusy: boolean
  connected: boolean
  training: boolean
  onConnect: () => void
  onStartTraining: (trainingForm: TrainingFormState) => void
}

const defaultTrainingForm: TrainingFormState = {
  hiddenLayersNumber: 1,
  activationFunctionName: 'logistic',
  learningRate: 0.0001,
  stopError: 0.001,
  maxEpochs: 200,
}

export function TrainingForm({
  connectionBusy,
  connected,
  training,
  onConnect,
  onStartTraining,
}: TrainingFormProps) {
  return (
    <form
      className="training-form"
      onSubmit={(event) => {
        event.preventDefault()
        onStartTraining(readTrainingForm(event.currentTarget))
      }}
    >
      <label>
        <span>Hidden layers</span>
        <input
          type="number"
          min="1"
          step="1"
          name="hiddenLayersNumber"
          defaultValue={defaultTrainingForm.hiddenLayersNumber}
          disabled={training}
          required
        />
      </label>

      <label>
        <span>Activation function</span>
        <select
          name="activationFunctionName"
          defaultValue={defaultTrainingForm.activationFunctionName}
          disabled={training}
        >
          <option value="logistic">Logistic</option>
          <option value="linear">Linear</option>
          <option value="hiperbolicTan">Hyperbolic tan</option>
          <option value="netOverTwo">Net over two</option>
        </select>
      </label>

      <label>
        <span>Learning rate</span>
        <input
          type="number"
          min="0"
          step="0.0001"
          name="learningRate"
          defaultValue={defaultTrainingForm.learningRate}
          disabled={training}
          required
        />
      </label>

      <label>
        <span>Stop error</span>
        <input
          type="number"
          min="0"
          step="0.0001"
          name="stopError"
          defaultValue={defaultTrainingForm.stopError}
          disabled={training}
          required
        />
      </label>

      <label>
        <span>Max epochs</span>
        <input
          type="number"
          min="1"
          step="1"
          name="maxEpochs"
          defaultValue={defaultTrainingForm.maxEpochs}
          disabled={training}
          required
        />
      </label>

      <div className="actions">
        <button
          type="button"
          onClick={onConnect}
          disabled={connectionBusy || connected}
        >
          {connected ? 'Connected' : 'Connect'}
        </button>
        <button type="submit" disabled={training || connectionBusy}>
          {training ? 'Training...' : 'Start mushrooms training'}
        </button>
      </div>
    </form>
  )
}

function readTrainingForm(form: HTMLFormElement): TrainingFormState {
  const formData = new FormData(form)

  return {
    hiddenLayersNumber: readNumber(formData, 'hiddenLayersNumber'),
    activationFunctionName: readString(formData, 'activationFunctionName'),
    learningRate: readNumber(formData, 'learningRate'),
    stopError: readNumber(formData, 'stopError'),
    maxEpochs: readNumber(formData, 'maxEpochs'),
  }
}

function readNumber(formData: FormData, fieldName: string): number {
  const value = Number(formData.get(fieldName))

  if (!Number.isFinite(value)) {
    throw new Error(`Invalid numeric form value: ${fieldName}`)
  }

  return value
}

function readString(formData: FormData, fieldName: string): string {
  const value = formData.get(fieldName)

  if (typeof value !== 'string' || value.length === 0) {
    throw new Error(`Invalid form value: ${fieldName}`)
  }

  return value
}
