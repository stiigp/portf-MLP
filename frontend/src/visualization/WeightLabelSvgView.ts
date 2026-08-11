import { createSvgElement, formatNumber } from './mlpSvgDom'

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
