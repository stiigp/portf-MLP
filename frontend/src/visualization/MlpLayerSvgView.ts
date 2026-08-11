import type { LayerTopology } from '../types/TrainingEvent'
import { createSvgElement } from './mlpSvgDom'
import { NeuronSvgView } from './NeuronSvgView'
import type {
  LayerLayout,
  MlpLayoutState,
  MlpSvgSnapshot,
} from './mlpSvgTypes'

export class MlpLayerSvgView {
  readonly group = createSvgElement('g', 'mlp-layer')
  private readonly rail = createSvgElement('line', 'mlp-layer-rail')
  private readonly label = createSvgElement('text', 'mlp-layer-label')
  private readonly neuronViews = new Map<string, NeuronSvgView>()

  constructor() {
    this.group.append(this.rail, this.label)
  }

  update(
    layerLayout: LayerLayout,
    layout: MlpLayoutState,
    snapshot: MlpSvgSnapshot,
  ): void {
    const { layer, x, y, height } = layerLayout
    const activeNeuronIds = new Set(layer.perceptronIds)

    this.group.dataset.layerType = layer.type
    this.rail.setAttribute('x1', String(x))
    this.rail.setAttribute('x2', String(x))
    this.rail.setAttribute('y1', String(y - 20))
    this.rail.setAttribute('y2', String(y + height + 20))
    this.label.setAttribute('x', String(x))
    this.label.setAttribute('y', '44')
    this.label.textContent = this.layerTitle(layer)

    for (const neuronId of layer.perceptronIds) {
      const position = layout.neurons.get(neuronId)

      if (!position) {
        continue
      }

      let neuronView = this.neuronViews.get(neuronId)

      if (!neuronView) {
        neuronView = new NeuronSvgView(neuronId)
        this.neuronViews.set(neuronId, neuronView)
        this.group.append(neuronView.group)
      }

      neuronView.update({
        id: neuronId,
        kind: layer.type,
        position,
        selected: snapshot.selectedNeuronId === neuronId,
        output: snapshot.outputs.find((output) => output.id === neuronId),
      })
    }

    for (const [neuronId, neuronView] of this.neuronViews) {
      if (!activeNeuronIds.has(neuronId)) {
        neuronView.destroy()
        this.neuronViews.delete(neuronId)
      }
    }
  }

  destroy(): void {
    this.group.remove()
    this.neuronViews.clear()
  }

  private layerTitle(layer: LayerTopology): string {
    if (layer.type === 'hidden') {
      return `Hidden ${layer.index + 1}`
    }

    return layer.type === 'input' ? 'Input' : 'Output'
  }
}
