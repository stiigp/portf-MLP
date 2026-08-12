import type { ConnectionSnapshot } from '../types/TrainingEvent'
import { clamp, connectionKey, createSvgElement } from './mlpSvgDom'
import type { NeuronLayout } from './mlpSvgTypes'
import { WeightLabelSvgView } from './WeightLabelSvgView'

type ConnectionUpdate = {
  connection: ConnectionSnapshot
  from: NeuronLayout
  to: NeuronLayout
  highlighted: boolean
  labelsVisible: boolean
}

export class ConnectionSvgView {
  readonly group = createSvgElement('g', 'mlp-connection')
  private readonly path = createSvgElement('path', 'mlp-connection-path')
  private readonly hitArea = createSvgElement('path', 'mlp-connection-hit-area')
  private readonly label = new WeightLabelSvgView()
  readonly labelGroup = this.label.group

  constructor(initialConnection: ConnectionSnapshot) {
    this.group.dataset.connection = connectionKey(initialConnection)
    this.group.append(this.path, this.hitArea)
  }

  update(update: ConnectionUpdate): void {
    const { connection, from, to } = update
    const key = connectionKey(connection)
    const curveOffset = Math.max(40, Math.abs(to.x - from.x) * 0.42)
    const pathData = [
      `M ${from.x + from.radius + 2} ${from.y}`,
      `C ${from.x + curveOffset} ${from.y}`,
      `${to.x - curveOffset} ${to.y}`,
      `${to.x - to.radius - 2} ${to.y}`,
    ].join(' ')
    const magnitude = clamp(Math.abs(connection.weight), 0.2, 4)

    this.group.dataset.connection = key
    this.group.dataset.sign =
      connection.weight > 0 ? 'positive' : connection.weight < 0 ? 'negative' : 'zero'
    this.group.dataset.highlighted = String(update.highlighted)

    this.path.setAttribute('d', pathData)
    this.path.setAttribute('stroke-width', String(0.8 + magnitude * 0.8))
    this.hitArea.setAttribute('d', pathData)

    this.label.update({
      key,
      value: connection.weight,
      x: (from.x + to.x) / 2,
      y: (from.y + to.y) / 2,
      visible: update.labelsVisible,
    })
  }

  destroy(): void {
    this.group.remove()
    this.labelGroup.remove()
  }
}
