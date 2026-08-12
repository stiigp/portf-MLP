import type { LayerTopology, OutputValueSnapshot } from '../types/TrainingEvent'
import { createSvgElement, formatNumber } from './mlpSvgDom'
import type { NeuronLayout } from './mlpSvgTypes'

type NeuronUpdate = {
  id: string
  kind: LayerTopology['type']
  position: NeuronLayout
  selected: boolean
  output?: OutputValueSnapshot
  onSelect?: (id: string) => void
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
    this.idLabel.setAttribute('fill', 'black')
    this.valueLabel.setAttribute('fill', 'black')
    this.group.append(this.halo, this.circle, this.idLabel, this.valueLabel)
  }

  update(update: NeuronUpdate): void {
    const { id, kind, position, output } = update
    const radius = position.radius
    const outputError = output ? Math.abs(output.output - output.expected) : 0

    this.group.dataset.kind = kind
    this.group.dataset.selected = String(update.selected)
    this.group.dataset.error =
      output && outputError > 0.25 ? 'high' : output ? 'low' : 'none'
    this.group.setAttribute('transform', `translate(${position.x} ${position.y})`)
    this.group.onclick = () => update.onSelect?.(id)

    this.halo.setAttribute('r', String(radius + 7))
    this.circle.setAttribute('r', String(radius))

    this.idLabel.setAttribute('y', '5')
    this.idLabel.textContent = displayNeuronId(id, kind)

    this.valueLabel.setAttribute('y', '4')
    this.valueLabel.textContent = output
      ? `${formatNumber(output.output)}`
      : ''
  }

  destroy(): void {
    this.group.remove()
  }
}

function displayNeuronId(id: string, kind: LayerTopology['type']): string {
  if (kind === 'output') {
    return ''
  }

  if (kind === 'hidden') {
    const hiddenNeuronIndex = id.split('-').at(-1)

    return hiddenNeuronIndex ? `h-${hiddenNeuronIndex}` : id
  }

  return id
 }
