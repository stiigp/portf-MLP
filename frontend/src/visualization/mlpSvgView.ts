import type { LayerTopology } from '../types/TrainingEvent'
import { ConnectionSvgView } from './ConnectionSvgView'
import { MlpLayerSvgView } from './MlpLayerSvgView'
import { MlpLayoutEngine } from './MlpLayoutEngine'
import { OutputPanelSvgView } from './OutputPanelSvgView'
import { TrainingProgressSvgView } from './TrainingProgressSvgView'
import { connectionKey, createSvgElement } from './mlpSvgDom'
import {
  VIEWBOX_HEIGHT,
  VIEWBOX_WIDTH,
  type MlpLayoutState,
  type MlpSvgSnapshot,
} from './mlpSvgTypes'

export type { MlpSvgSnapshot } from './mlpSvgTypes'

type MlpSvgViewOptions = {
  onNeuronSelect?: (id: string) => void
}

export class MlpSvgView {
  private readonly svg: SVGSVGElement
  private readonly layoutEngine = new MlpLayoutEngine()
  private readonly connectionGroup = createSvgElement('g', 'mlp-connections')
  private readonly layerGroup = createSvgElement('g', 'mlp-layers')
  private readonly overlayGroup = createSvgElement('g', 'mlp-overlays')
  private readonly layerViews = new Map<string, MlpLayerSvgView>()
  private readonly connectionViews = new Map<string, ConnectionSvgView>()
  private readonly outputPanel = new OutputPanelSvgView()
  private readonly progressView = new TrainingProgressSvgView()
  private readonly options: MlpSvgViewOptions

  constructor(
    svg: SVGSVGElement,
    options: MlpSvgViewOptions = {},
  ) {
    this.svg = svg
    this.options = options
    this.svg.setAttribute('viewBox', `0 0 ${VIEWBOX_WIDTH} ${VIEWBOX_HEIGHT}`)
    this.svg.setAttribute('role', 'img')
    this.svg.setAttribute(
      'aria-label',
      'Static multilayer perceptron visualization',
    )
    this.svg.append(this.connectionGroup, this.layerGroup, this.overlayGroup)
    this.overlayGroup.append(this.outputPanel.group, this.progressView.group)
  }

  update(snapshot: MlpSvgSnapshot): void {
    const layout = this.layoutEngine.compute(snapshot.topology)
    this.updateConnections(snapshot, layout)
    this.updateLayers(snapshot, layout)
    this.outputPanel.update(snapshot.outputs, layout)
    this.progressView.update(snapshot, layout)
  }

  destroy(): void {
    this.svg.replaceChildren()
    this.layerViews.clear()
    this.connectionViews.clear()
  }

  private updateLayers(
    snapshot: MlpSvgSnapshot,
    layout: MlpLayoutState,
  ): void {
    const activeLayerKeys = new Set<string>()

    for (const layerLayout of layout.layers) {
      const key = `${layerLayout.layer.type}-${layerLayout.layer.index}`
      activeLayerKeys.add(key)

      let layerView = this.layerViews.get(key)

      if (!layerView) {
        layerView = new MlpLayerSvgView({
          onNeuronSelect: this.options.onNeuronSelect,
        })
        this.layerViews.set(key, layerView)
        this.layerGroup.append(layerView.group)
      }

      layerView.update(layerLayout, layout, snapshot)
    }

    for (const [key, layerView] of this.layerViews) {
      if (!activeLayerKeys.has(key)) {
        layerView.destroy()
        this.layerViews.delete(key)
      }
    }
  }

  private updateConnections(
    snapshot: MlpSvgSnapshot,
    layout: MlpLayoutState,
  ): void {
    const activeConnectionKeys = new Set<string>()

    for (const connection of snapshot.connections) {
      const key = connectionKey(connection)
      const from = layout.neurons.get(connection.from)
      const to = layout.neurons.get(connection.to)

      if (!from || !to) {
        continue
      }

      activeConnectionKeys.add(key)

      let connectionView = this.connectionViews.get(key)

      if (!connectionView) {
        connectionView = new ConnectionSvgView(connection)
        this.connectionViews.set(key, connectionView)
        this.connectionGroup.append(connectionView.group)
      }

      const selectedIncomingConnection = snapshot.selectedNeuronId === connection.to

      connectionView.update({
        connection,
        from,
        to,
        highlighted: selectedIncomingConnection,
        labelsVisible: selectedIncomingConnection,
      })
    }

    for (const [key, connectionView] of this.connectionViews) {
      if (!activeConnectionKeys.has(key)) {
        connectionView.destroy()
        this.connectionViews.delete(key)
      }
    }
  }
}

export function createStaticMlpSnapshot(): MlpSvgSnapshot {
  const topology: LayerTopology[] = [
    {
      type: 'input',
      index: 0,
      perceptronIds: ['i-0', 'i-1', 'i-2', 'i-3'],
    },
    {
      type: 'hidden',
      index: 0,
      perceptronIds: ['h-0-0', 'h-0-1', 'h-0-2', 'h-0-3', 'h-0-4'],
    },
    {
      type: 'hidden',
      index: 1,
      perceptronIds: ['h-1-0', 'h-1-1', 'h-1-2'],
    },
    {
      type: 'output',
      index: 0,
      perceptronIds: ['o-0', 'o-1'],
    },
  ]

  return {
    topology,
    connections: [
      { from: 'i-0', to: 'h-0-0', weight: 0.72 },
      { from: 'i-0', to: 'h-0-1', weight: -0.28 },
      { from: 'i-0', to: 'h-0-3', weight: 1.16 },
      { from: 'i-1', to: 'h-0-0', weight: -1.45 },
      { from: 'i-1', to: 'h-0-2', weight: 0.36 },
      { from: 'i-1', to: 'h-0-4', weight: 0.94 },
      { from: 'i-2', to: 'h-0-1', weight: 1.88 },
      { from: 'i-2', to: 'h-0-2', weight: -0.62 },
      { from: 'i-2', to: 'h-0-4', weight: 0.24 },
      { from: 'i-3', to: 'h-0-0', weight: 0.41 },
      { from: 'i-3', to: 'h-0-3', weight: -1.08 },
      { from: 'h-0-0', to: 'h-1-0', weight: 1.34 },
      { from: 'h-0-0', to: 'h-1-1', weight: -0.33 },
      { from: 'h-0-1', to: 'h-1-0', weight: -0.87 },
      { from: 'h-0-1', to: 'h-1-2', weight: 0.56 },
      { from: 'h-0-2', to: 'h-1-1', weight: 1.62 },
      { from: 'h-0-3', to: 'h-1-0', weight: 0.19 },
      { from: 'h-0-3', to: 'h-1-2', weight: -1.27 },
      { from: 'h-0-4', to: 'h-1-1', weight: 0.77 },
      { from: 'h-1-0', to: 'o-0', weight: 1.9 },
      { from: 'h-1-0', to: 'o-1', weight: -0.55 },
      { from: 'h-1-1', to: 'o-0', weight: -1.18 },
      { from: 'h-1-1', to: 'o-1', weight: 1.43 },
      { from: 'h-1-2', to: 'o-0', weight: 0.48 },
      { from: 'h-1-2', to: 'o-1', weight: -1.72 },
    ],
    outputs: [
      {
        id: 'o-0',
        net: 1.38,
        output: 0.799,
        expected: 1,
      },
      {
        id: 'o-1',
        net: -0.91,
        output: 0.287,
        expected: 0,
      },
    ],
    progress: {
      type: 'TRAINING_PROGRESS',
      sessionId: 'static-preview',
      epoch: 8,
      sampleIndex: 142,
      networkError: 0.03782,
    },
    status: 'RUNNING',
    eventType: 'WEIGHTS_UPDATE',
    selectedNeuronId: 'o-0',
  }
}
