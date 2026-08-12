import { useEffect, useMemo, useRef, useState } from 'react'

import {
  createStaticMlpSnapshot,
  MlpSvgView,
} from '../visualization/mlpSvgView'

export function StaticMlpVisualization() {
  const svgRef = useRef<SVGSVGElement | null>(null)
  const viewRef = useRef<MlpSvgView | null>(null)
  const initialSnapshot = useMemo(() => createStaticMlpSnapshot(), [])
  const [selectedNeuronId, setSelectedNeuronId] = useState<string | null>(
    initialSnapshot.selectedNeuronId ?? null,
  )
  const snapshot = useMemo(
    () => ({
      ...initialSnapshot,
      selectedNeuronId,
    }),
    [initialSnapshot, selectedNeuronId],
  )

  useEffect(() => {
    if (!svgRef.current) {
      return
    }

    const view = new MlpSvgView(svgRef.current, {
      onNeuronSelect: setSelectedNeuronId,
    })
    viewRef.current = view

    return () => {
      view.destroy()
      viewRef.current = null
    }
  }, [])

  useEffect(() => {
    viewRef.current?.update(snapshot)
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
