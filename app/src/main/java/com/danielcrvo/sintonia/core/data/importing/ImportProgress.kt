package com.danielcrvo.sintonia.core.data.importing

enum class ImportStep(val weight: Int) {
    Connect(5),
    Live(25),
    Movies(30),
    Series(30),
    Guide(10),
}

enum class StepStatus { Pending, Running, Done, Error }

data class StepState(
    val step: ImportStep,
    val status: StepStatus = StepStatus.Pending,
    val count: Int = 0,
    val fraction: Float = 0f,
)

data class ImportProgress(
    val steps: List<StepState> = ImportStep.entries.map { StepState(it) },
    val percent: Int = 0,
) {
    fun update(step: ImportStep, status: StepStatus, count: Int? = null, fraction: Float? = null): ImportProgress {
        val updated = steps.map {
            if (it.step != step) {
                it
            } else {
                it.copy(
                    status = status,
                    count = count ?: it.count,
                    fraction = fraction ?: if (status == StepStatus.Done) 1f else it.fraction,
                )
            }
        }
        val raw = updated.sumOf { state ->
            val share = when (state.status) {
                StepStatus.Done -> 1f
                StepStatus.Running -> state.fraction.coerceIn(0f, 1f)
                else -> 0f
            }
            (state.step.weight * share).toDouble()
        }
        return copy(steps = updated, percent = maxOf(percent, raw.toInt().coerceAtMost(100)))
    }
}
