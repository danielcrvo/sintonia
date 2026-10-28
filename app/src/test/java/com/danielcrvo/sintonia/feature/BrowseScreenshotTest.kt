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
import com.danielcrvo.sintonia.core.data.SearchResults
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.Episode
import com.danielcrvo.sintonia.core.model.Movie
import com.danielcrvo.sintonia.core.model.PlayerRequest
import com.danielcrvo.sintonia.core.model.ProgressKind
import com.danielcrvo.sintonia.core.model.Season
import com.danielcrvo.sintonia.core.model.Series
import com.danielcrvo.sintonia.core.model.SeriesInfo
import com.danielcrvo.sintonia.core.model.VodInfo
import com.danielcrvo.sintonia.core.model.WatchProgress
import com.danielcrvo.sintonia.feature.home.ui.ContinueItem
import com.danielcrvo.sintonia.feature.home.ui.FeaturedItem
import com.danielcrvo.sintonia.feature.home.ui.HomeScreen
import com.danielcrvo.sintonia.feature.home.ui.HomeUiState
import com.danielcrvo.sintonia.feature.movies.ui.InfoState
import com.danielcrvo.sintonia.feature.movies.ui.MovieDetailScreen
import com.danielcrvo.sintonia.feature.movies.ui.MovieDetailUiState
import com.danielcrvo.sintonia.feature.player.ui.NextEpisodeCard
import com.danielcrvo.sintonia.feature.player.ui.UpNext
import com.danielcrvo.sintonia.feature.search.ui.SearchScreen
import com.danielcrvo.sintonia.feature.search.ui.SearchUiState
import com.danielcrvo.sintonia.feature.series.ui.SeriesDetailScreen
import com.danielcrvo.sintonia.feature.series.ui.SeriesDetailUiState
import com.danielcrvo.sintonia.feature.series.ui.SeriesInfoState
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
class BrowseScreenshotTest {
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

    private val movie = Movie(1, "m1", "Horizonte Perdido", null, "aventura", "2026", 7.8, "mp4", null)
    private val episodes = (1..4).map { Episode("e$it", 1, it, listOf("Começo", "A viagem", "Tempestade", "Chegada")[it - 1], 2700 + it * 60, null, "mp4") }
    private val seasons = listOf(Season(1, episodes), Season(2, listOf(Episode("e21", 2, 1, "Volta", 2800, null, "mp4"))))

    @Test
    fun movieDetail() = shot("movie-detail") {
        MovieDetailScreen(
            state = MovieDetailUiState(
                loading = false,
                movie = movie,
                info = InfoState.Loaded(
                    VodInfo(
                        "Uma expedição improvável atravessa o deserto em busca de uma cidade que ninguém jamais viu. No caminho, o grupo descobre que o mapa guarda mais perguntas do que respostas.",
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
            onBack = noop,
            onPlay = noop,
            onRetryInfo = noop,
        )
    }

    @Test
    fun seriesDetail() = shot("series-detail") {
        SeriesDetailScreen(
            state = SeriesDetailUiState(
                loading = false,
                series = Series(2, "s1", "Rua das Estrelas", null, "drama", "2024", 8.1, null),
                info = SeriesInfoState.Loaded(SeriesInfo("Quatro vizinhos descobrem um segredo no fim da rua.", "Ana Prado", null, "Drama", null, seasons)),
                progress = mapOf(
                    "e2" to WatchProgress(ProgressKind.Episode, "e2", 900_000, 2_820_000, 5, null, null, "s1", 1, 2, "mp4"),
                    "e1" to WatchProgress(ProgressKind.Episode, "e1", 2_700_000, 2_760_000, 4, null, null, "s1", 1, 1, "mp4"),
                ),
            ),
            onBack = noop,
            onPlayEpisode = {},
            onPlaySingle = noop,
            onSeasonSelected = {},
            onRetryInfo = noop,
        )
    }

    @Test
    fun searchResults() = shot("search-results") {
        SearchScreen(
            typed = "ho",
            onQueryChange = {},
            onCancel = noop,
            onChannelClick = {},
            onMovieClick = {},
            onSeriesClick = {},
            state = SearchUiState(
                "ho",
                SearchResults(
                    channels = listOf(Channel(1, "1", 1, "Rede Horizonte", null, null, null, null)),
                    movies = listOf(movie),
                    series = listOf(Series(2, "s1", "Horas Vagas", null, null, "2024", null, null)),
                ),
                searched = true,
            ),
        )
    }

    @Test
    fun searchEmpty() = shot("search-empty") {
        SearchScreen("xyz", SearchUiState("xyz", SearchResults(), searched = true), {}, noop, {}, {}, {})
    }

    @Test
    fun home() = shot("home") {
        HomeScreen(
            state = HomeUiState(
                loaded = true,
                featured = FeaturedItem(1, "Horizonte Perdido", "2026", null),
                continueWatching = persistentListOf(
                    ContinueItem("a", "Rua das Estrelas", 1, 2, null, 0.32f, PlayerRequest.Movie(1)),
                    ContinueItem("b", "Eco Distante", null, null, null, 0.7f, PlayerRequest.Movie(2)),
                ),
                favorites = persistentListOf(
                    Channel(1, "1", 1, "Rede Horizonte", null, null, null, null),
                    Channel(2, "2", 2, "Canal Aurora", null, null, null, null),
                    Channel(3, "3", 3, "Arena Sport", null, null, null, null),
                ),
            ),
            onFeatured = {},
            onContinue = {},
            onChannel = {},
        )
    }

    @Test
    fun nextEpisode() = shot("next-episode") {
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black), Alignment.BottomEnd) {
            NextEpisodeCard(UpNext(episodes[1], 8), onCancel = noop, onPlayNow = noop)
        }
    }
}
