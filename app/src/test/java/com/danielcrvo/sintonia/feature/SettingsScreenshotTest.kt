package com.danielcrvo.sintonia.feature

import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.danielcrvo.sintonia.core.designsystem.components.DialogSurface
import com.danielcrvo.sintonia.core.designsystem.components.SheetSurface
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.IptvList
import com.danielcrvo.sintonia.core.model.ListType
import com.danielcrvo.sintonia.core.parental.PinError
import com.danielcrvo.sintonia.core.parental.PinPromptState
import com.danielcrvo.sintonia.core.parental.PinPurpose
import com.danielcrvo.sintonia.core.parental.ui.PinPromptContent
import com.danielcrvo.sintonia.feature.parental.ui.BlockableCategory
import com.danielcrvo.sintonia.feature.parental.ui.ParentalScreen
import com.danielcrvo.sintonia.feature.parental.ui.ParentalSection
import com.danielcrvo.sintonia.feature.parental.ui.ParentalUiState
import com.danielcrvo.sintonia.feature.settings.ui.ListsScreen
import com.danielcrvo.sintonia.feature.settings.ui.ListsUiState
import com.danielcrvo.sintonia.feature.settings.ui.SettingsScreen
import com.danielcrvo.sintonia.feature.settings.ui.SettingsUiState
import com.danielcrvo.sintonia.testing.saveScreenshot
import kotlinx.collections.immutable.persistentListOf
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
class SettingsScreenshotTest {
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
    private val list = IptvList(1, "Minha lista", ListType.Xtream, "http://s", "ana", "m3u8", null, 1240, 5300, 310, 0, 1_790_000_000_000)
    private val second = list.copy(id = 2, name = "Lista do sítio", type = ListType.M3uUrl, channelCount = 86, lastImportedAt = null)

    @Test
    fun settings() = shot("settings") {
        SettingsScreen(
            SettingsUiState(activeList = list, parentalEnabled = true),
            "1.0.0",
            onBack = noop,
            onSwitchList = noop,
            onRefresh = noop,
            onThemeSelected = {},
            onOpenSheet = {},
            onAutoUpdateToggle = {},
            onOpenParental = noop,
            onOpenCategories = noop,
        )
    }

    @Test
    fun lists() = shot("lists") {
        ListsScreen(
            ListsUiState(persistentListOf(list, second), activeId = 1),
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }

    @Test
    fun removeDialog() = shot("lists-remove") {
        Box(Modifier.fillMaxSize().background(SintoniaTheme.colors.scrim), Alignment.Center) {
            DialogSurface(title = "Remover lista") {
                com.danielcrvo.sintonia.core.designsystem.components
                    .SText("A lista “Minha lista” e seus favoritos e progresso serão apagados deste aparelho.")
            }
        }
    }

    private val sections = persistentListOf(
        ParentalSection(ContentKind.Live, persistentListOf(BlockableCategory("a", "Notícias", false), BlockableCategory("b", "Adultos", true))),
        ParentalSection(
            ContentKind.Movie,
            persistentListOf(BlockableCategory("c", "Ação", false), BlockableCategory("d", "Terror", true), BlockableCategory("e", "Drama", false)),
        ),
        ParentalSection(ContentKind.Series, persistentListOf()),
    )

    @Test
    fun parental() = shot("parental") {
        ParentalScreen(
            ParentalUiState(enabled = true, unlocked = true, sections = sections, expanded = ContentKind.Movie),
            {},
            {},
            noop,
            {},
            { _, _, _ -> },
        )
    }

    @Test
    fun pinCreate() = shot("pin-create") {
        Box(Modifier.fillMaxSize().background(SintoniaTheme.colors.scrim), Alignment.BottomCenter) {
            SheetSurface(onDismiss = noop) { PinPromptContent(PinPromptState(PinPurpose.Create, digits = "12"), {}, {}) }
        }
    }

    @Test
    fun pinError() = shot("pin-error") {
        Box(Modifier.fillMaxSize().background(SintoniaTheme.colors.scrim), Alignment.BottomCenter) {
            SheetSurface(onDismiss = noop) {
                PinPromptContent(PinPromptState(PinPurpose.Unlock, error = PinError.Wrong, errorTick = 1), {}, {})
            }
        }
    }
}
