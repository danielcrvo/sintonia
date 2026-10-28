package com.danielcrvo.sintonia.core.data.parser

import com.danielcrvo.sintonia.core.model.ContentKind
import java.text.Normalizer

object M3uClassifier {
    private val MovieExtension = Regex("""\.(mp4|mkv|avi|mov)$""")
    private val SeriesGroup = Regex("serie|temporada|season")
    private val MovieGroup = Regex("filme|movie|vod")

    fun classify(entry: M3uEntry): ContentKind {
        val url = entry.url.lowercase()
        if ("/movie/" in url) return ContentKind.Movie
        if ("/series/" in url) return ContentKind.Series

        val group = normalize(entry.group)
        if (SeriesGroup.containsMatchIn(group)) return ContentKind.Series
        if (MovieGroup.containsMatchIn(group)) return ContentKind.Movie

        val path = url.substringBefore('?').substringBefore('#')
        return if (MovieExtension.containsMatchIn(path)) ContentKind.Movie else ContentKind.Live
    }

    private fun normalize(text: String?): String =
        Normalizer.normalize(text.orEmpty(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
}
