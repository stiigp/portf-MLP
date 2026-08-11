import type { OutputValueSnapshot } from '../types/TrainingEvent'
import { clamp, createSvgElement, formatNumber } from './mlpSvgDom'

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
