package com.danielcrvo.sintonia.feature

import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.core.app.ApplicationProvider
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.data.importing.ImportProgress
import com.danielcrvo.sintonia.core.data.importing.ImportStep
import com.danielcrvo.sintonia.core.data.importing.StepStatus
import com.danielcrvo.sintonia.core.designsystem.components.SEmptyState
import com.danielcrvo.sintonia.core.designsystem.components.SheetSurface
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.Episode
import com.danielcrvo.sintonia.core.model.Movie
import com.danielcrvo.sintonia.core.model.Program
import com.danielcrvo.sintonia.core.model.ProgressKind
import com.danielcrvo.sintonia.core.model.Season
import com.danielcrvo.sintonia.core.model.Series
import com.danielcrvo.sintonia.core.model.SeriesInfo
import com.danielcrvo.sintonia.core.model.VodInfo
import com.danielcrvo.sintonia.core.model.WatchProgress
import com.danielcrvo.sintonia.core.parental.PinError
import com.danielcrvo.sintonia.core.parental.PinPromptState
import com.danielcrvo.sintonia.core.parental.PinPurpose
import com.danielcrvo.sintonia.core.parental.ui.PinPromptContent
import com.danielcrvo.sintonia.feature.lists.ui.addlist.AddListScreen
import com.danielcrvo.sintonia.feature.lists.ui.addlist.AddListUiState
import com.danielcrvo.sintonia.feature.lists.ui.importing.ImportScreen
import com.danielcrvo.sintonia.feature.lists.ui.importing.ImportUiState
import com.danielcrvo.sintonia.feature.live.ui.LiveScreen
import com.danielcrvo.sintonia.feature.live.ui.LiveUiState
import com.danielcrvo.sintonia.feature.movies.ui.InfoState
import com.danielcrvo.sintonia.feature.movies.ui.MovieDetailScreen
import com.danielcrvo.sintonia.feature.movies.ui.MovieDetailUiState
import com.danielcrvo.sintonia.feature.series.ui.SeriesDetailScreen
import com.danielcrvo.sintonia.feature.series.ui.SeriesDetailUiState
import com.danielcrvo.sintonia.feature.series.ui.SeriesInfoState
import com.danielcrvo.sintonia.feature.welcome.ui.WelcomeScreen
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
@Config(sdk = [35], qualifiers = "pt-w960dp-h540dp-tvdpi")
class TvScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun disableAnimations() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private val now = 1_800_000_000_000L
    private val min = 60_000L

    private val channels = listOf(
        Channel(1, "1", 1, "Rede Horizonte", null, "geral", "rede.h", null),
        Channel(2, "2", 2, "Canal Aurora", null, "geral", "aurora", null),
        Channel(3, "3", 3, "Arena Sport 1", null, "esportes", "arena1", null),
        Channel(4, "4", 4, "Faro Notícias", null, "noticias", null, null),
        Channel(5, "5", 5, "Cine Estrela", null, "filmes", "cine", null),
    )
    private val guide = mapOf(
        "rede.h" to listOf(
            Program(now - 50 * min, now + 30 * min, "Jornal da Noite", "Resumo do dia com as principais notícias."),
            Program(now + 30 * min, now + 90 * min, "Novela das Nove", null),
            Program(now + 90 * min, now + 150 * min, "Cinema em Casa", null),
        ),
        "aurora" to listOf(
            Program(now - 20 * min, now + 70 * min, "Música ao vivo", null),
            Program(now + 70 * min, now + 130 * min, "Sessão de Clipes", null),
        ),
        "arena1" to listOf(
            Program(now - 50 * min, now + 10 * min, "Final do Campeonato", "Transmissão ao vivo da grande final."),
            Program(now + 10 * min, now + 100 * min, "Melhores Momentos", null),
        ),
    )

    @Test
    fun liveGuide() {
        val mode = mutableStateOf(ThemeMode.Dark)
        rule.setContent {
            SintoniaTheme(mode = mode.value, deviceClass = DeviceClass.Tv) {
                val items = remember { flowOf(PagingData.from(channels)) }.collectAsLazyPagingItems()
                LiveScreen(
                    state = LiveUiState(
                        playingChannelId = 1,
                        categories = persistentListOf(Category("geral", "Geral", ContentKind.Live, 120)),
                    ),
                    channels = items,
                    programs = mutableStateOf(emptyMap()),
                    nowMillis = now,
                    onTabSelected = {},
                    onCategorySelected = {},
                    onFilterClick = {},
                    onChannelClick = {},
                    onChannelLongClick = {},
                    onToggleFavorite = {},
                    onRequestProgram = {},
                    guide = mutableStateOf(guide),
                    onGuideWindow = {},
                    onRequestGuide = {},
                )
            }
        }
        rule.saveScreenshot("tv-live-guide-dark")
        mode.value = ThemeMode.Light
        rule.saveScreenshot("tv-live-guide-light")
    }

    private fun tvShot(
        name: String,
        verify: () -> Unit = {},
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        val mode = mutableStateOf(ThemeMode.Dark)
        rule.setContent { SintoniaTheme(mode = mode.value, deviceClass = DeviceClass.Tv) { content() } }
        verify()
        rule.saveScreenshot("$name-dark")
        mode.value = ThemeMode.Light
        rule.saveScreenshot("$name-light")
    }

    @Test
    fun importing() = tvShot(
        "tv-import-running",
        verify = {
            rule.onNodeWithText(rule.activity.getString(R.string.loading_title)).assertIsDisplayed()
            rule.onNodeWithText(rule.activity.getString(R.string.loading_cancel)).assertIsDisplayed()
        },
    ) {
        ImportScreen(
            ImportUiState(
                progress = ImportProgress()
                    .update(ImportStep.Connect, StepStatus.Done)
                    .update(ImportStep.Live, StepStatus.Done, count = 1240)
                    .update(ImportStep.Movies, StepStatus.Running, count = 5300, fraction = 0.45f),
            ),
            onCancel = {},
            onRetry = {},
            onEditDetails = {},
        )
    }

    @Test
    fun welcome() = tvShot(
        "tv-welcome",
        verify = {
            rule.onNodeWithText(rule.activity.getString(R.string.welcome_title)).assertIsDisplayed()
            rule.onNodeWithText(rule.activity.getString(R.string.welcome_add_list)).assertIsDisplayed()
        },
    ) { WelcomeScreen(onAddList = {}) }

    @Test
    fun addList() = tvShot(
        "tv-addlist-xtream",
        verify = { rule.onNodeWithText(rule.activity.getString(R.string.addlist_title)).assertIsDisplayed() },
    ) {
        AddListScreen(
            state = AddListUiState(name = "Minha lista", server = "http://servidor.exemplo:8080", username = "ana", password = "segredo"),
            onBack = {},
            onTabChange = {},
            onNameChange = {},
            onServerChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onUrlChange = {},
            onPickFile = {},
            onSubmit = {},
        )
    }

    @Test
    fun pinPrompt() = tvShot("tv-pin-error") {
        Box(Modifier.fillMaxSize().background(SintoniaTheme.colors.scrim), Alignment.BottomCenter) {
            SheetSurface(onDismiss = {}) {
                PinPromptContent(PinPromptState(PinPurpose.Unlock, error = PinError.Wrong, errorTick = 1), {}, {})
            }
        }
    }

    @Test
    fun emptyState() = tvShot("tv-empty-state") {
        Box(Modifier.fillMaxSize().background(SintoniaTheme.colors.background)) {
            SEmptyState(icon = R.drawable.ic_tv, title = "Sem canais", message = "Esta categoria ainda não tem canais.", code = "HTTP 404")
        }
    }

    private val movie = Movie(1, "m1", "Horizonte Perdido", null, "aventura", "2026", 7.8, "mp4", null)
    private val episodes = (1..4).map { Episode("e$it", 1, it, listOf("Começo", "A viagem", "Tempestade", "Chegada")[it - 1], 2700 + it * 60, null, "mp4") }
    private val seasons = listOf(Season(1, episodes), Season(2, listOf(Episode("e21", 2, 1, "Volta", 2800, null, "mp4"))))

    @Test
    fun movieDetail() = tvShot("tv-movie-detail") {
        MovieDetailScreen(
            state = MovieDetailUiState(
                loading = false,
                movie = movie,
                info = InfoState.Loaded(
                    VodInfo(
                        "Uma expedição improvável atravessa o deserto em busca de uma cidade que ninguém jamais viu.",
                        "Ana Prado, Beto Lima, Clara Nunes",
                        "Davi Rocha",
                        "Aventura, Drama",
                        6300,
                        null,
                        "mp4",
                    ),
                ),
                progress = WatchProgress(ProgressKind.Movie, "m1", 2_530_000, 6_300_000, 1, null, null, null, null, null, null),
            ),
            onBack = {},
            onPlay = {},
            onRetryInfo = {},
        )
    }

    @Test
    fun seriesDetail() = tvShot("tv-series-detail") {
        SeriesDetailScreen(
            state = SeriesDetailUiState(
                loading = false,
                series = Series(2, "s1", "Rua das Estrelas", null, "drama", "2024", 8.1, null),
                info = SeriesInfoState.Loaded(SeriesInfo("Quatro vizinhos descobrem um segredo no fim da rua.", "Ana Prado", null, "Drama", null, seasons)),
                progress = mapOf("e2" to WatchProgress(ProgressKind.Episode, "e2", 900_000, 2_820_000, 5, null, null, "s1", 1, 2, "mp4")),
            ),
            onBack = {},
            onPlayEpisode = {},
            onPlaySingle = {},
            onSeasonSelected = {},
            onRetryInfo = {},
        )
    }
}
