import { createSvgElement, formatNumber } from './mlpSvgDom'
import type { MlpLayoutState, MlpSvgSnapshot } from './mlpSvgTypes'

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
