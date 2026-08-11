import type { OutputValueSnapshot } from '../types/TrainingEvent'
import { createSvgElement } from './mlpSvgDom'
import type { MlpLayoutState } from './mlpSvgTypes'
import { OutputRowSvgView } from './OutputRowSvgView'

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
