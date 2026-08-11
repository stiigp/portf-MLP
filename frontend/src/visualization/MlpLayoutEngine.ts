import type { LayerTopology } from '../types/TrainingEvent'
import {
  VIEWBOX_HEIGHT,
  VIEWBOX_WIDTH,
  type MlpLayoutState,
  type Point,
} from './mlpSvgTypes'

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
    const networkWidth = this.width - margin.left - margin.right
    const networkHeight = this.height - margin.top - margin.bottom
    const layerGap =
      topology.length > 1 ? networkWidth / (topology.length - 1) : 0
    const neurons = new Map<string, Point>()

    const layers = topology.map((layer, layerPosition) => {
      const x = margin.left + layerPosition * layerGap
      const neuronGap =
        layer.perceptronIds.length > 1
          ? networkHeight / (layer.perceptronIds.length - 1)
          : 0

      layer.perceptronIds.forEach((id, neuronIndex) => {
        const y =
          layer.perceptronIds.length === 1
            ? margin.top + networkHeight / 2
            : margin.top + neuronIndex * neuronGap

        neurons.set(id, { x, y })
      })

      return {
        layer,
        x,
        y: margin.top,
        height: networkHeight,
      }
    })

    return {
      width: this.width,
      height: this.height,
      layers,
      neurons,
    }
  }
}
