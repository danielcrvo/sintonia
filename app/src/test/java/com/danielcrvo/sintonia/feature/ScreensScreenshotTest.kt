package com.danielcrvo.sintonia.feature

import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.danielcrvo.sintonia.core.data.PickedFile
import com.danielcrvo.sintonia.core.data.importing.ImportProgress
import com.danielcrvo.sintonia.core.data.importing.ImportStep
import com.danielcrvo.sintonia.core.data.importing.StepStatus
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.network.ApiException
import com.danielcrvo.sintonia.feature.lists.domain.FieldError
import com.danielcrvo.sintonia.feature.lists.domain.FormField
import com.danielcrvo.sintonia.feature.lists.ui.addlist.AddListScreen
import com.danielcrvo.sintonia.feature.lists.ui.addlist.AddListTab
import com.danielcrvo.sintonia.feature.lists.ui.addlist.AddListUiState
import com.danielcrvo.sintonia.feature.lists.ui.importing.ImportFailure
import com.danielcrvo.sintonia.feature.lists.ui.importing.ImportScreen
import com.danielcrvo.sintonia.feature.lists.ui.importing.ImportUiState
import com.danielcrvo.sintonia.feature.welcome.ui.WelcomeScreen
import com.danielcrvo.sintonia.testing.saveScreenshot
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "pt-w400dp-h800dp-xxhdpi")
class ScreensScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun disableAnimations() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        val mode = mutableStateOf(ThemeMode.Dark)
        rule.setContent { SintoniaTheme(mode = mode.value) { content() } }
        rule.saveScreenshot("$name-dark")
        mode.value = ThemeMode.Light
        rule.saveScreenshot("$name-light")
    }

    private val noop = {}

    @Test
    fun welcome() = shot("welcome") { WelcomeScreen(onAddList = noop) }

    @Composable
    private fun addList(state: AddListUiState) = AddListScreen(
        state = state,
        onBack = noop,
        onTabChange = {},
        onNameChange = {},
        onServerChange = {},
        onUsernameChange = {},
        onPasswordChange = {},
        onUrlChange = {},
        onPickFile = noop,
        onSubmit = noop,
    )

    @Test
    fun addListXtream() = shot("addlist-xtream") {
        addList(
            AddListUiState(
                name = "Minha lista",
                server = "http://servidor.exemplo:8080",
                username = "ana",
                password = "segredo",
                errors = mapOf(FormField.Server to FieldError.InvalidUrl),
            ),
        )
    }

    @Test
    fun addListUrl() = shot("addlist-url") { addList(AddListUiState(tab = AddListTab.Url, url = "http://lista.exemplo/a.m3u")) }

    @Test
    fun addListFileEmpty() = shot("addlist-file-empty") { addList(AddListUiState(tab = AddListTab.File)) }

    @Test
    fun addListFilePicked() = shot("addlist-file-picked") {
        addList(AddListUiState(tab = AddListTab.File, file = PickedFile("/x", "canais-2026.m3u", 1_240_000, 3820)))
    }

    private val running = ImportProgress()
        .update(ImportStep.Connect, StepStatus.Done)
        .update(ImportStep.Live, StepStatus.Done, count = 1240)
        .update(ImportStep.Movies, StepStatus.Running, count = 5300, fraction = 0.45f)

    @Test
    fun importing() = shot("import-running") {
        ImportScreen(ImportUiState(progress = running), onCancel = noop, onRetry = noop, onEditDetails = noop)
    }

    @Test
    fun importError() = shot("import-error") {
        ImportScreen(
            ImportUiState(progress = running, failure = ImportFailure(ApiException.Kind.Auth, "servidor.exemplo", null)),
            onCancel = noop,
            onRetry = noop,
            onEditDetails = noop,
        )
    }
}
