import { useEffect, useMemo, useRef, useState, type MouseEvent } from 'react'

import type {
  ConnectionSnapshot,
  LayerTopology,
  OutputValueSnapshot,
} from '../types/TrainingEvent'
import { MlpSvgView } from '../visualization/mlpSvgView'
import type { MlpSvgSnapshot } from '../visualization/mlpSvgTypes'

type MlpVisualizationProps = {
  topology: LayerTopology[]
  connections: ConnectionSnapshot[]
  outputs: OutputValueSnapshot[]
}

export function MlpVisualization({
  topology,
  connections,
  outputs,
}: MlpVisualizationProps) {
  const svgRef = useRef<SVGSVGElement | null>(null)
  const viewRef = useRef<MlpSvgView | null>(null)
  const [selectedNeuronId, setSelectedNeuronId] = useState<string | null>(null)
  const hasTopology = topology.length > 0
  const visibleSelectedNeuronId =
    selectedNeuronId &&
    topology.some((layer) => layer.perceptronIds.includes(selectedNeuronId))
      ? selectedNeuronId
      : null

  const snapshot = useMemo<MlpSvgSnapshot>(
    () => ({
      topology,
      connections,
      outputs,
      progress: null,
      status: null,
      eventType: null,
      selectedNeuronId: visibleSelectedNeuronId,
    }),
    [connections, outputs, visibleSelectedNeuronId, topology],
  )

  useEffect(() => {
    if (!svgRef.current || !hasTopology) {
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
  }, [hasTopology])

  useEffect(() => {
    if (hasTopology) {
      viewRef.current?.update(snapshot)
    }
  }, [hasTopology, snapshot])

  function handleVisualizationClick(event: MouseEvent<SVGSVGElement>): void {
    const target = event.target

    if (target instanceof Element && target.closest('.mlp-neuron')) {
      return
    }

    setSelectedNeuronId(null)
  }

  return (
    <section className="mlp-visualization-section">
      {hasTopology ? (
        <svg
          ref={svgRef}
          className="mlp-visualization"
          onClick={handleVisualizationClick}
        />
      ) : (
        <div className="mlp-visualization mlp-visualization-empty">
          <span>No MLP topology yet</span>
        </div>
      )}
    </section>
  )
}
