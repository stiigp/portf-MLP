import { useState } from 'react'

export interface TrainingFormState {
  databaseName: string
  hiddenLayersNumber: number
  activationFunctionName: string
  learningRate: number
  stopError: number
  maxEpochs: number
}

interface TrainingFormProps {
  connectionBusy: boolean
  training: boolean
  onStartTraining: (trainingForm: TrainingFormState) => void
}

const defaultTrainingForm: TrainingFormState = {
  databaseName: 'fruits',
  hiddenLayersNumber: 1,
  activationFunctionName: 'hyperbolicTan',
  learningRate: 0.001,
  stopError: 0.001,
  maxEpochs: 2000,
}

export function TrainingForm({
  connectionBusy,
  training,
  onStartTraining,
}: TrainingFormProps) {
  const [hiddenLayersNumber, setHiddenLayersNumber] = useState(
    defaultTrainingForm.hiddenLayersNumber,
  )
  const [maxEpochs, setMaxEpochs] = useState(defaultTrainingForm.maxEpochs)

  return (
    <form
      className="training-form"
      onSubmit={(event) => {
        event.preventDefault()
        onStartTraining(readTrainingForm(event.currentTarget))
      }}
    >
      <fieldset className="radio-field">
        <legend>Database</legend>
        <label className="radio-option">
          <input
            type="radio"
            name="databaseName"
            value="fruits"
            defaultChecked={defaultTrainingForm.databaseName === 'fruits'}
            disabled={training}
          />
          <span>Fruits</span>
        </label>
        <label className="radio-option">
          <input
            type="radio"
            name="databaseName"
            value="mushrooms"
            defaultChecked={defaultTrainingForm.databaseName === 'mushrooms'}
            disabled={training}
          />
          <span>Mushrooms</span>
        </label>
      </fieldset>

      <label className="range-field">
        <span>
          Hidden layers <strong>{hiddenLayersNumber}</strong>
        </span>
        <input
          type="range"
          min="1"
          max="3"
          step="1"
          name="hiddenLayersNumber"
          value={hiddenLayersNumber}
          onChange={(event) =>
            setHiddenLayersNumber(Number(event.currentTarget.value))
          }
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
          <option value="hiperbolicTan">Hyperbolic tan</option>
          <option value="logistic">Logistic</option>
          <option value="linear">Linear</option>
          <option value="netOverTwo">Net over two</option>
        </select>
      </label>

      <label>
        <span>Learning rate</span>
        <input
          type="number"
          min="0"
          step="0.001"
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

      <label className="range-field">
        <span>
          Max epochs <strong>{maxEpochs}</strong>
        </span>
        <input
          type="range"
          min="100"
          max="4000"
          step="100"
          name="maxEpochs"
          value={maxEpochs}
          onChange={(event) => setMaxEpochs(Number(event.currentTarget.value))}
          disabled={training}
          required
        />
      </label>

      <div className="actions">
        <button type="submit" disabled={training || connectionBusy}>
          {training ? 'Training...' : 'Start training'}
        </button>
      </div>
    </form>
  )
}

function readTrainingForm(form: HTMLFormElement): TrainingFormState {
  const formData = new FormData(form)

  return {
    databaseName: readString(formData, 'databaseName'),
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
