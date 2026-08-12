import { useEffect, useMemo, useRef, useState } from 'react'

import type {
  ConnectionSnapshot,
  LayerTopology,
  OutputValueSnapshot,
  TrainingEventType,
  TrainingFinishedEvent,
  TrainingProgressEvent,
} from '../types/TrainingEvent'
import { MlpSvgView } from '../visualization/mlpSvgView'
import type { MlpSvgSnapshot } from '../visualization/mlpSvgTypes'

type MlpVisualizationProps = {
  topology: LayerTopology[]
  connections: ConnectionSnapshot[]
  outputs: OutputValueSnapshot[]
  progress: TrainingProgressEvent | TrainingFinishedEvent | null
  eventType: TrainingEventType | null
}

export function MlpVisualization({
  topology,
  connections,
  outputs,
  progress,
  eventType,
}: MlpVisualizationProps) {
  const svgRef = useRef<SVGSVGElement | null>(null)
  const viewRef = useRef<MlpSvgView | null>(null)
  const [selectedNeuronId, setSelectedNeuronId] = useState<string | null>(null)
  const hasTopology = topology.length > 0

  const snapshot = useMemo<MlpSvgSnapshot>(
    () => ({
      topology,
      connections,
      outputs,
      progress,
      status: null,
      eventType,
      selectedNeuronId,
    }),
    [connections, eventType, outputs, progress, selectedNeuronId, topology],
  )

  useEffect(() => {
    if (
      selectedNeuronId &&
      !topology.some((layer) => layer.perceptronIds.includes(selectedNeuronId))
    ) {
      setSelectedNeuronId(null)
    }
  }, [selectedNeuronId, topology])

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

  return (
    <section className="mlp-visualization-section">
      <div className="mlp-visualization-heading">
        <h2>MLP visualization</h2>
        <p>Live SVG view of the training topology, weights, and outputs.</p>
      </div>

      {hasTopology ? (
        <svg ref={svgRef} className="mlp-visualization" />
      ) : (
        <div className="mlp-visualization mlp-visualization-empty">
          <span>No MLP topology yet</span>
        </div>
      )}
    </section>
  )
}
