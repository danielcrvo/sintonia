package com.danielcrvo.sintonia.core.common

import java.text.NumberFormat
import java.util.Locale

fun formatCount(count: Int, locale: Locale = Locale.getDefault()): String = NumberFormat.getIntegerInstance(locale).format(count)

fun formatRating(rating: Double, locale: Locale = Locale.getDefault()): String = "★ " + String.format(locale, "%.1f", rating)
