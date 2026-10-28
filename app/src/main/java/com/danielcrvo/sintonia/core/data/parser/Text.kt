package com.danielcrvo.sintonia.core.data.parser

import java.text.Normalizer

private val Diacritics = Regex("\\p{M}+")

fun nameKey(name: String): String =
    Normalizer.normalize(name, Normalizer.Form.NFD).replace(Diacritics, "").lowercase()

fun escapeLike(query: String): String =
    query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
