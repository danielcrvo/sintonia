package com.danielcrvo.sintonia.core.data.parser

import com.danielcrvo.sintonia.core.model.ContentKind
import kotlin.test.Test
import kotlin.test.assertEquals

class M3uClassifierTest {
    private fun entry(url: String, group: String? = null) = M3uEntry(null, null, "x", null, group, url)

    @Test
    fun `URL path decides first`() {
        assertEquals(ContentKind.Movie, M3uClassifier.classify(entry("http://s/movie/u/p/1.mp4", "Canais")))
        assertEquals(ContentKind.Series, M3uClassifier.classify(entry("http://s/series/u/p/1.mkv", "Filmes")))
    }

    @Test
    fun `group with different accents or case also matches`() {
        assertEquals(ContentKind.Series, M3uClassifier.classify(entry("http://s/1", "SÉRIES | Drama")))
        assertEquals(ContentKind.Series, M3uClassifier.classify(entry("http://s/1", "Temporada 2")))
        assertEquals(ContentKind.Movie, M3uClassifier.classify(entry("http://s/1", "Filmes de Ação")))
        assertEquals(ContentKind.Movie, M3uClassifier.classify(entry("http://s/1", "VOD Premium")))
    }

    @Test
    fun `video extension becomes a movie and the rest is live`() {
        assertEquals(ContentKind.Movie, M3uClassifier.classify(entry("http://s/arquivo.mkv?token=1")))
        assertEquals(ContentKind.Live, M3uClassifier.classify(entry("http://s/live/1.m3u8")))
        assertEquals(ContentKind.Live, M3uClassifier.classify(entry("http://s/1.ts", "Notícias")))
    }
}
