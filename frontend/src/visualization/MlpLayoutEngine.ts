import type { LayerTopology } from '../types/TrainingEvent'
import {
  VIEWBOX_HEIGHT,
  VIEWBOX_WIDTH,
  type MlpLayoutState,
  type NeuronLayout,
} from './mlpSvgTypes'

const PREFERRED_NEURON_RADIUS = 18
const MIN_NEURON_RADIUS = 8
const MIN_NEURON_GAP = 8

export class MlpLayoutEngine {
  private readonly width: number
  private readonly height: number

  constructor(width = VIEWBOX_WIDTH, height = VIEWBOX_HEIGHT) {
    this.width = width
    this.height = height
  }

  compute(topology: LayerTopology[]): MlpLayoutState {
    const margin = {
      top: 92,
      right: 250,
      bottom: 70,
      left: 74,
    }
    const maxLayerSize = Math.max(
      1,
      ...topology.map((layer) => layer.perceptronIds.length),
    )
    const preferredNetworkHeight =
      maxLayerSize > 1
        ? (maxLayerSize - 1) *
          (PREFERRED_NEURON_RADIUS * 2 + MIN_NEURON_GAP)
        : 0
    const height = Math.max(
      this.height,
      margin.top + margin.bottom + preferredNetworkHeight,
    )
    const networkWidth = this.width - margin.left - margin.right
    const networkHeight = height - margin.top - margin.bottom
    const layerGap =
      topology.length > 1 ? networkWidth / (topology.length - 1) : 0
    const neurons = new Map<string, NeuronLayout>()

    const layers = topology.map((layer, layerPosition) => {
      const x = margin.left + layerPosition * layerGap
      const neuronGap =
        layer.perceptronIds.length > 1
          ? networkHeight / (layer.perceptronIds.length - 1)
          : 0
      const neuronRadius =
        layer.perceptronIds.length > 1
          ? clamp(
              (neuronGap - MIN_NEURON_GAP) / 2,
              MIN_NEURON_RADIUS,
              PREFERRED_NEURON_RADIUS,
            )
          : PREFERRED_NEURON_RADIUS

      layer.perceptronIds.forEach((id, neuronIndex) => {
        const y =
          layer.perceptronIds.length === 1
            ? margin.top + networkHeight / 2
            : margin.top + neuronIndex * neuronGap

        neurons.set(id, { x, y, radius: neuronRadius })
      })

      return {
        layer,
        x,
        y: margin.top,
        height: networkHeight,
        neuronRadius,
      }
    })

    return {
      width: this.width,
      height,
      layers,
      neurons,
    }
  }
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max)
}
