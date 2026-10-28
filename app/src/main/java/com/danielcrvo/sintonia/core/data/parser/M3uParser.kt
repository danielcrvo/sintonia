package com.danielcrvo.sintonia.core.data.parser

data class M3uEntry(
    val tvgId: String?,
    val tvgName: String?,
    val name: String,
    val logo: String?,
    val group: String?,
    val url: String,
)

class M3uParser {
    var epgUrl: String? = null
        private set

    fun parse(lines: Sequence<String>): Sequence<M3uEntry> = sequence {
        var attributes: Map<String, String> = emptyMap()
        var name = ""
        var extGroup: String? = null
        var first = true

        for (rawLine in lines) {
            val raw = if (first) rawLine.removePrefix("\uFEFF") else rawLine
            first = false
            val line = raw.trim()
            if (line.isEmpty()) continue
            when {
                line.startsWith("#EXTM3U") -> {
                    val header = parseAttributes(line)
                    epgUrl = (header["url-tvg"] ?: header["x-tvg-url"]).orNull()
                }
                line.startsWith("#EXTINF:") -> {
                    val body = line.removePrefix("#EXTINF:")
                    val comma = firstUnquotedComma(body)
                    attributes = parseAttributes(if (comma == -1) body else body.substring(0, comma))
                    name = if (comma == -1) "" else body.substring(comma + 1).trim()
                    extGroup = null
                }
                line.startsWith("#EXTGRP:") -> extGroup = line.removePrefix("#EXTGRP:").orNull()
                line.startsWith("#") -> Unit
                !UrlLine.containsMatchIn(line) -> Unit // text that is not a URL does not become an item
                else -> {
                    val tvgName = attributes["tvg-name"].orNull()
                    yield(
                        M3uEntry(
                            tvgId = attributes["tvg-id"].orNull(),
                            tvgName = tvgName,
                            name = name.ifEmpty { tvgName ?: line },
                            logo = attributes["tvg-logo"].orNull(),
                            group = attributes["group-title"].orNull() ?: extGroup,
                            url = line,
                        ),
                    )
                    attributes = emptyMap()
                    name = ""
                    extGroup = null
                }
            }
        }
    }

    private fun parseAttributes(text: String): Map<String, String> =
        Attribute.findAll(text).associate { it.groupValues[1].lowercase() to it.groupValues[2] }

    private fun firstUnquotedComma(text: String): Int {
        var quoted = false
        for (i in text.indices) {
            when (text[i]) {
                '"' -> quoted = !quoted
                ',' -> if (!quoted) return i
            }
        }
        return -1
    }

    private fun String?.orNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        val UrlLine = Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE)
        val Attribute = Regex("""([\w-]+)="([^"]*)"""")
    }
}
