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
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.core.app.ApplicationProvider
import com.danielcrvo.sintonia.core.data.ChannelSort
import com.danielcrvo.sintonia.core.designsystem.components.SheetSurface
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.Program
import com.danielcrvo.sintonia.core.player.PlaybackKind
import com.danielcrvo.sintonia.core.player.PlaybackSession
import com.danielcrvo.sintonia.core.player.PlaybackState
import com.danielcrvo.sintonia.core.player.PlaybackStatus
import com.danielcrvo.sintonia.core.player.TrackOption
import com.danielcrvo.sintonia.core.player.TrackOptions
import com.danielcrvo.sintonia.feature.live.ui.ChannelProgramsContent
import com.danielcrvo.sintonia.feature.live.ui.LiveFilterContent
import com.danielcrvo.sintonia.feature.live.ui.LiveScreen
import com.danielcrvo.sintonia.feature.live.ui.LiveTab
import com.danielcrvo.sintonia.feature.live.ui.LiveUiState
import com.danielcrvo.sintonia.feature.player.ui.PlayerActions
import com.danielcrvo.sintonia.feature.player.ui.PlayerPanel
import com.danielcrvo.sintonia.feature.player.ui.PlayerScreen
import com.danielcrvo.sintonia.feature.player.ui.PlayerUiState
import com.danielcrvo.sintonia.testing.saveScreenshot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf
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
class LiveScreenshotTest {
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

    private val now = 1_800_000_000_000L
    private val noop = {}

    private val channels = listOf(
        Channel(1, "1", 1, "Rede Horizonte", null, "geral", "rede.h", null),
        Channel(2, "2", 2, "Canal Aurora", null, "geral", "aurora", null),
        Channel(3, "3", 3, "Arena Sport 1", null, "esportes", "arena1", null),
        Channel(4, "4", 4, "Faro Notícias", null, "noticias", null, null),
        Channel(5, "5", 5, "Cine Estrela", null, "filmes", "cine", null),
    )
    private val programs = mapOf(
        "rede.h" to Program(now - 1_200_000, now + 1_800_000, "Jornal da Noite", null),
        "aurora" to Program(now - 600_000, now + 3_000_000, "Música ao vivo", null),
        "arena1" to Program(now - 3_000_000, now + 600_000, "Final do Campeonato", null),
    )
    private val categories = listOf(
        Category("geral", "Geral", ContentKind.Live, 120),
        Category("esportes", "Esportes", ContentKind.Live, 34),
        Category("noticias", "Notícias", ContentKind.Live, 18),
    )

    @Test
    fun liveList() = shot("live-list") {
        val items = androidx.compose.runtime
            .remember { flowOf(PagingData.from(channels)) }
            .collectAsLazyPagingItems()
        LiveScreen(
            state = LiveUiState(favoriteIds = setOf("2"), playingChannelId = 1, categories = persistentListOf(*categories.toTypedArray())),
            channels = items,
            programs = mutableStateOf(programs),
            nowMillis = now,
            onTabSelected = {},
            onCategorySelected = {},
            onFilterClick = noop,
            onChannelClick = {},
            onChannelLongClick = {},
            onToggleFavorite = {},
            onRequestProgram = {},
            guide = mutableStateOf(emptyMap()),
            onGuideWindow = {},
            onRequestGuide = {},
        )
    }

    @Test
    fun liveFavoritesEmpty() = shot("live-favorites-empty") {
        val items = androidx.compose.runtime
            .remember { flowOf(PagingData.from(emptyList<Channel>())) }
            .collectAsLazyPagingItems()
        LiveScreen(
            state = LiveUiState(tab = LiveTab.Favorites),
            channels = items,
            programs = mutableStateOf(emptyMap()),
            nowMillis = now,
            onTabSelected = {},
            onCategorySelected = {},
            onFilterClick = noop,
            onChannelClick = {},
            onChannelLongClick = {},
            onToggleFavorite = {},
            onRequestProgram = {},
            guide = mutableStateOf(emptyMap()),
            onGuideWindow = {},
            onRequestGuide = {},
        )
    }

    @Test
    fun filterSheet() = shot("live-filter-sheet") {
        Box(Modifier.fillMaxSize().background(SintoniaTheme.colors.scrim), Alignment.BottomCenter) {
            SheetSurface(onDismiss = noop, title = "Filtrar", scrollable = false) {
                LiveFilterContent(ChannelSort.Number, categories, "esportes", {}, {}, noop)
            }
        }
    }

    @Test
    fun programsSheet() = shot("live-programs-sheet") {
        Box(Modifier.fillMaxSize().background(SintoniaTheme.colors.scrim), Alignment.BottomCenter) {
            SheetSurface(onDismiss = noop, scrollable = false) {
                ChannelProgramsContent(
                    channels[0],
                    listOf(
                        Program(now - 7_200_000, now - 1_200_000, "Bom dia, Horizonte", null),
                        Program(now - 1_200_000, now + 1_800_000, "Jornal da Noite", null),
                        Program(now + 1_800_000, now + 5_400_000, "Cine Horizonte", null),
                    ),
                    true,
                    now,
                    {},
                    noop,
                )
            }
        }
    }

    private val actions = PlayerActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

    private fun liveState(panel: PlayerPanel? = null, status: PlaybackStatus = PlaybackStatus.Playing) = PlayerUiState(
        playback = PlaybackState(
            status = status,
            session = PlaybackSession(PlaybackKind.Live, 1, "1", "Rede Horizonte", null, null, "http://x", channelId = 1),
        ),
        tracks = TrackOptions(
            audio = listOf(TrackOption("1:0", "Português", "pt", true), TrackOption("1:1", "English", "en", false)),
            text = listOf(TrackOption("3:0", "Português", "pt", false)),
        ),
        panel = panel,
        channel = channels[0],
        favorite = true,
        canZap = true,
        nowProgram = programs.getValue("rede.h"),
        nextProgram = Program(now + 1_800_000, now + 5_400_000, "Cine Horizonte", null),
    )

    @Test
    fun playerLive() = shot("player-live") {
        PlayerScreen(liveState(), mutableStateOf(0L), actions) {}
    }

    @Test
    fun playerTracks() = shot("player-tracks") {
        PlayerScreen(liveState(PlayerPanel.Tracks), mutableStateOf(0L), actions) {}
    }

    @Test
    fun playerError() = shot("player-error") {
        PlayerScreen(
            liveState(
                status = PlaybackStatus.Error,
            ).let { it.copy(playback = it.playback.copy(error = com.danielcrvo.sintonia.core.player.PlaybackErrorKind.Network)) },
            mutableStateOf(0L),
            actions,
        ) {}
    }
}
