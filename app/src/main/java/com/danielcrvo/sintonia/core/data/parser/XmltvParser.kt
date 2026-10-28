package com.danielcrvo.sintonia.core.data.parser

import java.io.Reader
import java.time.LocalDateTime
import java.time.ZoneOffset

data class Programme(
    val channel: String,
    val start: Long,
    val stop: Long,
    val title: String,
    val description: String?,
)

object XmltvParser {
    private const val ChunkSize = 64 * 1024
    private const val Open = "<programme"
    private const val Close = "</programme>"

    /** The grid shows two description lines; the rest would only bloat the database. */
    private const val MaxDescriptionChars = 200

    /** An oversized programme is garbage or an attack: drop it to bound memory. */
    private const val MaxProgrammeChars = 256 * 1024

    fun parse(reader: Reader, accept: (channel: String, start: Long, stop: Long) -> Boolean = { _, _, _ -> true }): Sequence<Programme> = sequence {
        val buffer = StringBuilder()
        val chunk = CharArray(ChunkSize)
        val ready = ArrayList<Programme>()
        while (true) {
            val read = reader.read(chunk)
            if (read > 0) buffer.append(chunk, 0, read)
            val consumed = drain(buffer, final = read < 0, ready, accept)
            if (consumed > 0) buffer.delete(0, consumed)
            for (programme in ready) yield(programme)
            ready.clear()
            if (read < 0) break
        }
    }

    @Suppress("LoopWithTooManyJumpStatements") // buffer scan: splitting into functions would hide the cursor
    private fun drain(
        buffer: StringBuilder,
        final: Boolean,
        out: MutableList<Programme>,
        accept: (String, Long, Long) -> Boolean,
    ): Int {
        var cursor = 0
        while (true) {
            val open = buffer.indexOf(Open, cursor)
            if (open == -1) return if (final) buffer.length else maxOf(cursor, buffer.length - Open.length)
            val tagEnd = buffer.indexOf(">", open)
            if (tagEnd == -1) {
                if (final) return buffer.length
                if (buffer.length - open > MaxProgrammeChars) {
                    cursor = open + Open.length
                    continue
                }
                return open
            }
            if (buffer[tagEnd - 1] == '/') {
                cursor = tagEnd + 1 // <programme .../> without content
                continue
            }
            val close = buffer.indexOf(Close, tagEnd)
            if (close == -1) {
                if (final) return buffer.length
                if (buffer.length - open > MaxProgrammeChars) {
                    cursor = open + Open.length // no end in sight: skip this block and look for the next
                    continue
                }
                return open
            }
            cursor = close + Close.length

            val attributes = buffer.substring(open + Open.length, tagEnd)
            val channel = attribute(attributes, ChannelAttr) ?: continue
            val start = attribute(attributes, StartAttr)?.let(::parseDate) ?: continue
            val stop = attribute(attributes, StopAttr)?.let(::parseDate) ?: continue
            if (stop <= start || !accept(channel, start, stop)) continue
            val body = buffer.substring(tagEnd + 1, close)
            val title = firstTag(body, TitleTag) ?: continue
            out += Programme(channel, start, stop, title, firstTag(body, DescTag)?.take(MaxDescriptionChars))
        }
    }

    private val DatePattern =
        Regex("""^\s*(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})?\s*(?:([+-])(\d{2})(\d{2}))?\s*$""")

    fun parseDate(value: String): Long? {
        val m = DatePattern.matchEntire(value) ?: return null
        val g = m.groupValues
        return try {
            val utc = LocalDateTime
                .of(
                    g[1].toInt(),
                    g[2].toInt(),
                    g[3].toInt(),
                    g[4].toInt(),
                    g[5].toInt(),
                    g[6].ifEmpty { "0" }.toInt(),
                ).toInstantUtcMillis()
            val offset = if (g[7].isEmpty()) {
                0L
            } else {
                (if (g[7] == "-") -1 else 1) * (g[8].toLong() * 60 + g[9].toLong()) * 60_000
            }
            utc - offset
        } catch (_: java.time.DateTimeException) {
            null
        }
    }

    private fun LocalDateTime.toInstantUtcMillis() = toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun tagRegex(tag: String) = Regex("<$tag(?:\\s[^>]*)?>([\\s\\S]*?)</$tag>", RegexOption.IGNORE_CASE)

    private val TitleTag = tagRegex("title")
    private val DescTag = tagRegex("desc")

    private fun firstTag(body: String, tag: Regex): String? {
        val match = tag.find(body) ?: return null
        return decode(match.groupValues[1]).takeIf { it.isNotEmpty() }
    }

    private fun attrRegex(name: String) = Regex("(?:^|\\s)$name\\s*=\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

    private val ChannelAttr = attrRegex("channel")
    private val StartAttr = attrRegex("start")
    private val StopAttr = attrRegex("stop")

    private fun attribute(attributes: String, name: Regex): String? {
        val match = name.find(attributes) ?: return null
        return decode(match.groupValues[1])
    }

    private val Named = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'")
    private val Cdata = Regex("<!\\[CDATA\\[([\\s\\S]*?)]]>")
    private val Entity = Regex("&(?:#(\\d+)|#x([0-9a-fA-F]+)|([a-zA-Z]+));")

    private fun decode(text: String): String {
        // CDATA is literal: protect its '&' before decoding entities.
        val protectedText = Cdata.replace(text) { it.groupValues[1].replace("&", "\u0000amp;") }
        return Entity
            .replace(protectedText) { m ->
                val (dec, hex, name) = m.destructured
                when {
                    dec.isNotEmpty() -> codePoint(dec.toIntOrNull(), m.value)
                    hex.isNotEmpty() -> codePoint(hex.toIntOrNull(16), m.value)
                    else -> Named[name.lowercase()] ?: m.value
                }
            }.replace("\u0000amp;", "&")
            .trim()
    }

    private fun codePoint(value: Int?, fallback: String): String =
        if (value != null && Character.isValidCodePoint(value)) String(Character.toChars(value)) else fallback
}
