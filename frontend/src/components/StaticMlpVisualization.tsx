import { useEffect, useMemo, useRef } from 'react'

import {
  createStaticMlpSnapshot,
  MlpSvgView,
} from '../visualization/mlpSvgView'

export function StaticMlpVisualization() {
  const svgRef = useRef<SVGSVGElement | null>(null)
  const viewRef = useRef<MlpSvgView | null>(null)
  const snapshot = useMemo(() => createStaticMlpSnapshot(), [])

  useEffect(() => {
    if (!svgRef.current) {
      return
    }

    const view = new MlpSvgView(svgRef.current)
    viewRef.current = view
    view.update(snapshot)

    return () => {
      view.destroy()
      viewRef.current = null
    }
  }, [snapshot])

  return (
    <section className="mlp-visualization-section">
      <div className="mlp-visualization-heading">
        <h2>MLP visualization</h2>
        <p>Static SVG preview using the same topology, weights, and output payload shapes sent by the backend.</p>
      </div>
      <svg ref={svgRef} className="mlp-visualization" />
    </section>
  )
}
