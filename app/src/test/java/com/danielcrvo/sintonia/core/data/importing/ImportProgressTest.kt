package com.danielcrvo.sintonia.core.data.importing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportProgressTest {
    @Test
    fun `starts at zero and weights the steps`() {
        var progress = ImportProgress()
        assertEquals(0, progress.percent)
        progress = progress.update(ImportStep.Connect, StepStatus.Done)
        assertEquals(5, progress.percent)
        progress = progress.update(ImportStep.Live, StepStatus.Running, count = 50, fraction = 0.5f)
        assertEquals(17, progress.percent)
        progress = progress.update(ImportStep.Live, StepStatus.Done)
        assertEquals(30, progress.percent)
    }

    @Test
    fun `percent never decreases`() {
        var progress = ImportProgress().update(ImportStep.Live, StepStatus.Running, fraction = 0.8f)
        val before = progress.percent
        progress = progress.update(ImportStep.Live, StepStatus.Running, fraction = 0.1f)
        assertTrue(progress.percent >= before)
    }

    @Test
    fun `all done reaches 100`() {
        var progress = ImportProgress()
        ImportStep.entries.forEach { progress = progress.update(it, StepStatus.Done) }
        assertEquals(100, progress.percent)
    }
}
