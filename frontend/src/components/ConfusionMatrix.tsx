interface ConfusionMatrixProps {
  classLabels: string[]
  classSampleTotals: number[]
  matrix: number[][]
}

export function ConfusionMatrix({
  classLabels,
  classSampleTotals,
  matrix,
}: ConfusionMatrixProps) {
  if (classLabels.length === 0) {
    return (
      <section className="confusion-matrix" aria-labelledby="confusion-matrix-title">
        <h2 id="confusion-matrix-title">Confusion matrix</h2>
        <p className="confusion-matrix-empty">
          Start a test to receive the class labels and populate the matrix.
        </p>
      </section>
    )
  }

  return (
    <section className="confusion-matrix" aria-labelledby="confusion-matrix-title">
      <h2 id="confusion-matrix-title">Confusion matrix</h2>
      <div className="confusion-matrix-scroll">
        <table>
          <thead>
            <tr>
              <th scope="col" className="confusion-matrix-corner">
                Actual \\ Predicted
              </th>
              {classLabels.map((label) => (
                <th key={label} scope="col" title={label}>
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {classLabels.map((label, expectedClassIndex) => {
              const row = matrix[expectedClassIndex] ?? []
              const processedRowTotal = row.reduce(
                (total, value) => total + value,
                0,
              )
              const rowTotal =
                classSampleTotals[expectedClassIndex] ?? processedRowTotal

              return (
                <tr key={label}>
                  <th scope="row" title={label}>
                    {label}
                  </th>
                  {classLabels.map((predictedLabel, predictedClassIndex) => {
                    const count = row[predictedClassIndex] ?? 0
                    const percentage = rowTotal === 0 ? 0 : (count / rowTotal) * 100
                    const result = getCellResult(
                      count,
                      expectedClassIndex === predictedClassIndex,
                    )

                    return (
                      <td
                        key={predictedLabel}
                        data-result={result}
                        aria-label={`${label} predicted as ${predictedLabel}: ${count}, ${formatPercentage(percentage)}`}
                      >
                        <strong>{count}</strong>
                        <span>{formatPercentage(percentage)}</span>
                      </td>
                    )
                  })}
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function getCellResult(count: number, isCorrect: boolean): string {
  if (count === 0) {
    return 'empty'
  }

  return isCorrect ? 'correct' : 'incorrect'
}

function formatPercentage(value: number): string {
  return `${Math.round(value)}%`
}
