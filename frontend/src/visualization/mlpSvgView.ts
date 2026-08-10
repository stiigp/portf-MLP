import type {
  ConnectionSnapshot,
  LayerTopology,
  OutputValueSnapshot,
  TrainingEventType,
  TrainingFinishedEvent,
  TrainingProgressEvent,
  TrainingSessionStatus,
} from '../types/TrainingEvent'

const SVG_NS = 'http://www.w3.org/2000/svg'
const VIEWBOX_WIDTH = 920
const VIEWBOX_HEIGHT = 520

type Point = {
  x: number
  y: number
}

type LayerLayout = {
  layer: LayerTopology
  x: number
  y: number
  height: number
}

export type MlpLayoutState = {
  width: number
  height: number
  layers: LayerLayout[]
  neurons: Map<string, Point>
}

export type MlpSvgSnapshot = {
  topology: LayerTopology[]
  connections: ConnectionSnapshot[]
  outputs: OutputValueSnapshot[]
  progress: TrainingProgressEvent | TrainingFinishedEvent | null
  status: TrainingSessionStatus | null
  eventType: TrainingEventType | null
  selectedNeuronId?: string | null
  selectedConnectionKey?: string | null
}

function createSvgElement<K extends keyof SVGElementTagNameMap>(
  tagName: K,
  className?: string,
): SVGElementTagNameMap[K] {
  const element = document.createElementNS(SVG_NS, tagName)

  if (className) {
    element.setAttribute('class', className)
  }

  return element
}

function connectionKey(connection: Pick<ConnectionSnapshot, 'from' | 'to'>) {
  return `${connection.from}->${connection.to}`
}

function formatNumber(value: number, precision = 3): string {
  return Number.isFinite(value) ? value.toPrecision(precision) : String(value)
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}

function neuronKindFromId(id: string): LayerTopology['type'] {
  if (id.startsWith('i-')) {
    return 'input'
  }

  if (id.startsWith('o-')) {
    return 'output'
  }

  return 'hidden'
}

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

  constructor(svg: SVGSVGElement) {
    this.svg = svg
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
        layerView = new MlpLayerSvgView()
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

      connectionView.update({
        connection,
        from,
        to,
        selected: snapshot.selectedConnectionKey === key,
        highlighted:
          snapshot.selectedNeuronId === connection.from ||
          snapshot.selectedNeuronId === connection.to,
        labelsVisible: snapshot.selectedConnectionKey === key,
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

type NeuronUpdate = {
  id: string
  kind: LayerTopology['type']
  position: Point
  selected: boolean
  output?: OutputValueSnapshot
}

export class NeuronSvgView {
  readonly group = createSvgElement('g', 'mlp-neuron')
  readonly id: string
  private readonly halo = createSvgElement('circle', 'mlp-neuron-halo')
  private readonly circle = createSvgElement('circle', 'mlp-neuron-circle')
  private readonly idLabel = createSvgElement('text', 'mlp-neuron-id')
  private readonly valueLabel = createSvgElement('text', 'mlp-neuron-value')

  constructor(id: string) {
    this.id = id
    this.group.append(this.halo, this.circle, this.idLabel, this.valueLabel)
  }

  update(update: NeuronUpdate): void {
    const { id, kind, position, output } = update
    const radius = kind === 'output' ? 24 : 20
    const outputError = output ? Math.abs(output.output - output.expected) : 0

    this.group.dataset.kind = kind
    this.group.dataset.selected = String(update.selected)
    this.group.dataset.error =
      output && outputError > 0.25 ? 'high' : output ? 'low' : 'none'
    this.group.setAttribute('transform', `translate(${position.x} ${position.y})`)

    this.halo.setAttribute('r', String(radius + 7))
    this.circle.setAttribute('r', String(radius))

    this.idLabel.setAttribute('y', output ? '-4' : '5')
    this.idLabel.textContent = id

    this.valueLabel.setAttribute('y', '13')
    this.valueLabel.textContent = output
      ? `${formatNumber(output.output)} / ${formatNumber(output.expected)}`
      : ''
  }

  destroy(): void {
    this.group.remove()
  }
}

type ConnectionUpdate = {
  connection: ConnectionSnapshot
  from: Point
  to: Point
  selected: boolean
  highlighted: boolean
  labelsVisible: boolean
}

export class ConnectionSvgView {
  readonly group = createSvgElement('g', 'mlp-connection')
  private readonly initialConnection: ConnectionSnapshot
  private readonly path = createSvgElement('path', 'mlp-connection-path')
  private readonly hitArea = createSvgElement('path', 'mlp-connection-hit-area')
  private readonly label = new WeightLabelSvgView()
  private previousWeight: number | null = null

  constructor(initialConnection: ConnectionSnapshot) {
    this.initialConnection = initialConnection
    this.group.dataset.connection = connectionKey(initialConnection)
    this.group.append(this.path, this.hitArea, this.label.group)
  }

  update(update: ConnectionUpdate): void {
    const { connection, from, to } = update
    const key = connectionKey(connection)
    const curveOffset = Math.max(40, Math.abs(to.x - from.x) * 0.42)
    const pathData = [
      `M ${from.x + 22} ${from.y}`,
      `C ${from.x + curveOffset} ${from.y}`,
      `${to.x - curveOffset} ${to.y}`,
      `${to.x - 22} ${to.y}`,
    ].join(' ')
    const magnitude = clamp(Math.abs(connection.weight), 0.2, 4)
    const recentlyUpdated =
      this.previousWeight !== null &&
      Math.abs(connection.weight - this.previousWeight) > 0.0001

    this.group.dataset.connection = key
    this.group.dataset.sign =
      connection.weight > 0 ? 'positive' : connection.weight < 0 ? 'negative' : 'zero'
    this.group.dataset.selected = String(update.selected)
    this.group.dataset.highlighted = String(update.highlighted)
    this.group.dataset.updated = String(recentlyUpdated)

    this.path.setAttribute('d', pathData)
    this.path.setAttribute('stroke-width', String(0.8 + magnitude * 0.8))
    this.hitArea.setAttribute('d', pathData)

    this.label.update({
      key,
      value: connection.weight,
      x: (from.x + to.x) / 2,
      y: (from.y + to.y) / 2,
      visible: update.labelsVisible || recentlyUpdated,
    })

    this.previousWeight = connection.weight
  }

  destroy(): void {
    this.group.remove()
  }
}

type WeightLabelUpdate = {
  key: string
  value: number
  x: number
  y: number
  visible: boolean
}

export class WeightLabelSvgView {
  readonly group = createSvgElement('g', 'mlp-weight-label')
  private readonly background = createSvgElement('rect', 'mlp-weight-label-bg')
  private readonly text = createSvgElement('text', 'mlp-weight-label-text')

  constructor() {
    this.group.append(this.background, this.text)
  }

  update(update: WeightLabelUpdate): void {
    const label = formatNumber(update.value, 4)
    const width = Math.max(48, label.length * 8)

    this.group.dataset.connection = update.key
    this.group.dataset.visible = String(update.visible)
    this.group.setAttribute('transform', `translate(${update.x} ${update.y - 12})`)
    this.background.setAttribute('x', String(-width / 2))
    this.background.setAttribute('y', '-15')
    this.background.setAttribute('width', String(width))
    this.background.setAttribute('height', '22')
    this.text.setAttribute('y', '1')
    this.text.textContent = label
  }
}

export class OutputPanelSvgView {
  readonly group = createSvgElement('g', 'mlp-output-panel')
  private readonly title = createSvgElement('text', 'mlp-panel-title')
  private readonly rows = new Map<string, OutputRowSvgView>()

  constructor() {
    this.group.append(this.title)
  }

  update(outputs: OutputValueSnapshot[], layout: MlpLayoutState): void {
    const activeOutputIds = new Set(outputs.map((output) => output.id))

    this.group.setAttribute('transform', `translate(${layout.width - 220} 112)`)
    this.title.textContent = 'Output values'

    outputs.forEach((output, index) => {
      let row = this.rows.get(output.id)

      if (!row) {
        row = new OutputRowSvgView(output.id)
        this.rows.set(output.id, row)
        this.group.append(row.group)
      }

      row.update(output, index)
    })

    for (const [id, row] of this.rows) {
      if (!activeOutputIds.has(id)) {
        row.destroy()
        this.rows.delete(id)
      }
    }
  }
}

export class OutputRowSvgView {
  readonly group = createSvgElement('g', 'mlp-output-row')
  private readonly id: string
  private readonly label = createSvgElement('text', 'mlp-output-row-label')
  private readonly barTrack = createSvgElement('rect', 'mlp-output-bar-track')
  private readonly barValue = createSvgElement('rect', 'mlp-output-bar-value')
  private readonly expectedMarker = createSvgElement('line', 'mlp-output-expected')
  private readonly value = createSvgElement('text', 'mlp-output-row-value')

  constructor(id: string) {
    this.id = id
    this.group.append(
      this.label,
      this.barTrack,
      this.barValue,
      this.expectedMarker,
      this.value,
    )
  }

  update(output: OutputValueSnapshot, index: number): void {
    const y = index * 58 + 30
    const barWidth = 128
    const outputWidth = clamp(output.output, 0, 1) * barWidth
    const expectedX = clamp(output.expected, 0, 1) * barWidth
    const error = Math.abs(output.output - output.expected)

    this.group.dataset.output = this.id
    this.group.dataset.error = error > 0.25 ? 'high' : 'low'
    this.group.setAttribute('transform', `translate(0 ${y})`)

    this.label.setAttribute('x', '0')
    this.label.setAttribute('y', '0')
    this.label.textContent = output.id

    this.barTrack.setAttribute('x', '0')
    this.barTrack.setAttribute('y', '10')
    this.barTrack.setAttribute('width', String(barWidth))
    this.barTrack.setAttribute('height', '8')

    this.barValue.setAttribute('x', '0')
    this.barValue.setAttribute('y', '10')
    this.barValue.setAttribute('width', String(outputWidth))
    this.barValue.setAttribute('height', '8')

    this.expectedMarker.setAttribute('x1', String(expectedX))
    this.expectedMarker.setAttribute('x2', String(expectedX))
    this.expectedMarker.setAttribute('y1', '7')
    this.expectedMarker.setAttribute('y2', '22')

    this.value.setAttribute('x', '0')
    this.value.setAttribute('y', '38')
    this.value.textContent = `out ${formatNumber(output.output)} exp ${formatNumber(output.expected)}`
  }

  destroy(): void {
    this.group.remove()
  }
}

export class TrainingProgressSvgView {
  readonly group = createSvgElement('g', 'mlp-progress')
  private readonly title = createSvgElement('text', 'mlp-panel-title')
  private readonly meta = createSvgElement('text', 'mlp-progress-meta')
  private readonly error = createSvgElement('text', 'mlp-progress-error')

  constructor() {
    this.group.append(this.title, this.meta, this.error)
  }

  update(snapshot: MlpSvgSnapshot, layout: MlpLayoutState): void {
    this.group.setAttribute('transform', `translate(${layout.width - 220} 336)`)
    this.title.textContent = 'Training sample'

    if (snapshot.progress) {
      this.meta.textContent = `epoch ${snapshot.progress.epoch} sample ${snapshot.progress.sampleIndex}`
      this.error.textContent = `error ${formatNumber(snapshot.progress.networkError, 5)}`
    } else {
      this.meta.textContent = snapshot.status ?? 'static preview'
      this.error.textContent = snapshot.eventType ?? 'no event'
    }

    this.meta.setAttribute('y', '30')
    this.error.setAttribute('y', '54')
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
    selectedConnectionKey: 'h-1-0->o-0',
  }
}
