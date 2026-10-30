package com.danielcrvo.sintonia.feature.lists

import com.danielcrvo.sintonia.core.data.importing.ListImporter
import com.danielcrvo.sintonia.feature.lists.ui.importing.ImportEffect
import com.danielcrvo.sintonia.feature.lists.ui.importing.ImportViewModel
import com.danielcrvo.sintonia.testing.FakeDownloader
import com.danielcrvo.sintonia.testing.FakeLists
import com.danielcrvo.sintonia.testing.FakeStore
import com.danielcrvo.sintonia.testing.FakeXtream
import com.danielcrvo.sintonia.testing.TempImportFiles
import com.danielcrvo.sintonia.testing.sampleList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val lists = FakeLists(sampleList())

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ImportViewModel(
        ListImporter(lists, FakeStore(), FakeXtream(), FakeDownloader(), TempImportFiles(folder.newFolder("tmp"))),
        lists,
    )

    @Test
    fun `cancelling more than once sends a single effect`() = runTest {
        val viewModel = viewModel()

        repeat(3) { viewModel.cancel() }

        assertEquals(ImportEffect.Cancelled, viewModel.effects.first())
        assertNull(withTimeoutOrNull(100) { viewModel.effects.first() })
    }

    @Test
    fun `editing the details after cancelling does nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.cancel()
        viewModel.editDetails()

        assertEquals(ImportEffect.Cancelled, viewModel.effects.first())
        assertNull(withTimeoutOrNull(100) { viewModel.effects.first() })
    }
}
