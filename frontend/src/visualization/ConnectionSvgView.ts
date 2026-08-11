import type { ConnectionSnapshot } from '../types/TrainingEvent'
import { clamp, connectionKey, createSvgElement } from './mlpSvgDom'
import type { Point } from './mlpSvgTypes'
import { WeightLabelSvgView } from './WeightLabelSvgView'

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
  private readonly path = createSvgElement('path', 'mlp-connection-path')
  private readonly hitArea = createSvgElement('path', 'mlp-connection-hit-area')
  private readonly label = new WeightLabelSvgView()
  private previousWeight: number | null = null

  constructor(initialConnection: ConnectionSnapshot) {
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
