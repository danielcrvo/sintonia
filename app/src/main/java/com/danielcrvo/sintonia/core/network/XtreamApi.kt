package com.danielcrvo.sintonia.core.network

import com.danielcrvo.sintonia.core.data.importing.CategoryDraft
import com.danielcrvo.sintonia.core.data.importing.ChannelDraft
import com.danielcrvo.sintonia.core.data.importing.MovieDraft
import com.danielcrvo.sintonia.core.data.importing.SeriesDraft
import com.danielcrvo.sintonia.core.model.Episode
import com.danielcrvo.sintonia.core.model.Season
import com.danielcrvo.sintonia.core.model.SeriesInfo
import com.danielcrvo.sintonia.core.model.VodInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeToSequence
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.InputStream
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class XtreamCredentials(val server: String, val username: String, val password: String)

data class XtreamAuth(val liveExtension: String, val epgUrl: String, val expiresAtMillis: Long?)

enum class XtreamAction(val value: String) {
    LiveCategories("get_live_categories"),
    LiveStreams("get_live_streams"),
    VodCategories("get_vod_categories"),
    VodStreams("get_vod_streams"),
    SeriesCategories("get_series_categories"),
    Series("get_series"),
}

object Xtream {
    fun normalizeServer(input: String): String {
        var value = input.trim()
        val scheme = Regex("^([a-z][a-z0-9+.-]*)://", RegexOption.IGNORE_CASE).find(value)
        value = if (scheme != null) {
            scheme.groupValues[1].lowercase() + "://" + value.substring(scheme.value.length)
        } else {
            "http://$value"
        }
        value = value.replace(Regex("[?#].*$"), "")
        value = value.replace(Regex("/player_api\\.php$", RegexOption.IGNORE_CASE), "")
        return value.trimEnd('/')
    }

    private fun enc(text: String) = URLEncoder.encode(text, "UTF-8").replace("+", "%20")

    fun apiUrl(creds: XtreamCredentials, action: String? = null, params: Map<String, String> = emptyMap()): String {
        val query = buildList {
            add("username=${enc(creds.username)}")
            add("password=${enc(creds.password)}")
            if (action != null) add("action=${enc(action)}")
            params.forEach { (key, value) -> add("${enc(key)}=${enc(value)}") }
        }
        return "${creds.server}/player_api.php?${query.joinToString("&")}"
    }

    fun epgUrl(creds: XtreamCredentials) =
        "${creds.server}/xmltv.php?username=${enc(creds.username)}&password=${enc(creds.password)}"

    fun playbackUrl(creds: XtreamCredentials, kind: String, externalId: String, extension: String) =
        "${creds.server}/$kind/${enc(creds.username)}/${enc(creds.password)}/$externalId.$extension"
}

interface XtreamRemote {
    suspend fun authenticate(creds: XtreamCredentials): XtreamAuth
    suspend fun download(creds: XtreamCredentials, action: XtreamAction, destination: File): File
    fun readItems(stream: InputStream): Sequence<JsonObject>
}

@Singleton
class XtreamApi
    @Inject
    constructor(
        private val http: HttpClient,
        private val json: Json,
    ) : XtreamRemote {
        override suspend fun authenticate(creds: XtreamCredentials): XtreamAuth {
            val host = hostOf(creds.server)
            val body = http.getText(Xtream.apiUrl(creds))
            val info = runCatching { json.parseToJsonElement(body).jsonObject["user_info"] as? JsonObject }.getOrNull()
                ?: throw ApiException(ApiException.Kind.Invalid, host)

            val status = info["status"].textOrNull()?.lowercase()
            if (info["auth"].textOrNull() != "1" || (status != null && status != "active")) {
                throw ApiException(ApiException.Kind.Auth, host)
            }
            val allowed = (info["allowed_output_formats"] as? JsonArray)?.mapNotNull { it.textOrNull() }.orEmpty()
            val liveExtension = if (allowed.isNotEmpty() && "m3u8" !in allowed && "ts" in allowed) "ts" else "m3u8"
            val expires = info["exp_date"]
                .textOrNull()
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
                ?.times(1000)
            return XtreamAuth(liveExtension, Xtream.epgUrl(creds), expires)
        }

        override suspend fun download(creds: XtreamCredentials, action: XtreamAction, destination: File): File =
            http.download(Xtream.apiUrl(creds, action.value), destination)

        @OptIn(ExperimentalSerializationApi::class)
        override fun readItems(stream: InputStream): Sequence<JsonObject> =
            json.decodeToSequence(stream, JsonObject.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)

        suspend fun vodInfo(creds: XtreamCredentials, vodId: String): VodInfo =
            XtreamMapper.vodInfo(getObject(creds, "get_vod_info", mapOf("vod_id" to vodId)))

        suspend fun seriesInfo(creds: XtreamCredentials, seriesId: String): SeriesInfo =
            XtreamMapper.seriesInfo(getObject(creds, "get_series_info", mapOf("series_id" to seriesId)))

        suspend fun getObject(creds: XtreamCredentials, action: String, params: Map<String, String>): JsonObject {
            val host = hostOf(creds.server)
            val text = http.getText(Xtream.apiUrl(creds, action, params), HttpClient.LongReadSeconds)
            return runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
                ?: throw ApiException(ApiException.Kind.Invalid, host)
        }
    }

/** Tolerant conversion: providers send numbers as text or as numbers. */
object XtreamMapper {
    fun vodInfo(body: JsonObject): VodInfo {
        val info = body["info"] as? JsonObject ?: JsonObject(emptyMap())
        val data = body["movie_data"] as? JsonObject
        return VodInfo(
            plot = info["plot"].textOrNull() ?: info["description"].textOrNull(),
            cast = info["cast"].textOrNull() ?: info["actors"].textOrNull(),
            director = info["director"].textOrNull(),
            genre = info["genre"].textOrNull(),
            durationSecs = info["duration_secs"]
                .textOrNull()
                ?.toDoubleOrNull()
                ?.toInt()
                ?.takeIf { it > 0 },
            backdropUrl = firstImage(info["backdrop_path"]) ?: info["movie_image"].textOrNull(),
            containerExtension = data?.get("container_extension").textOrNull(),
        )
    }

    fun seriesInfo(body: JsonObject): SeriesInfo {
        val info = body["info"] as? JsonObject ?: JsonObject(emptyMap())
        val episodes = body["episodes"]
        // Providers send an object {"1":[...],"2":[...]} or sometimes a list of lists.
        val groups: List<Pair<Int, JsonArray>> = when (episodes) {
            is JsonObject -> episodes.entries.mapNotNull { (key, value) ->
                key.toIntOrNull()?.let { number -> (value as? JsonArray)?.let { number to it } }
            }
            is JsonArray -> episodes.mapIndexedNotNull { index, value -> (value as? JsonArray)?.let { index + 1 to it } }
            else -> emptyList()
        }
        val seasons = groups
            .mapNotNull { (number, rows) ->
                val list = rows
                    .mapIndexedNotNull { index, element ->
                        val row = element as? JsonObject ?: return@mapIndexedNotNull null
                        val id = row["id"].textOrNull() ?: return@mapIndexedNotNull null
                        val details = row["info"] as? JsonObject
                        val episodeNumber = row["episode_num"].intOrNull() ?: (index + 1)
                        Episode(
                            externalId = id,
                            season = number,
                            number = episodeNumber,
                            title = row["title"].textOrNull() ?: episodeNumber.toString(),
                            durationSecs = details
                                ?.get("duration_secs")
                                .textOrNull()
                                ?.toDoubleOrNull()
                                ?.toInt()
                                ?.takeIf { it > 0 },
                            imageUrl = details?.get("movie_image").textOrNull(),
                            containerExtension = row["container_extension"].textOrNull(),
                        )
                    }.sortedBy { it.number }
                if (list.isEmpty()) null else Season(number, list)
            }.sortedBy { it.number }
        return SeriesInfo(
            plot = info["plot"].textOrNull(),
            cast = info["cast"].textOrNull(),
            director = info["director"].textOrNull(),
            genre = info["genre"].textOrNull(),
            backdropUrl = firstImage(info["backdrop_path"]) ?: info["cover"].textOrNull(),
            seasons = seasons,
        )
    }

    private fun firstImage(element: JsonElement?): String? =
        if (element is JsonArray) element.firstOrNull().textOrNull() else element.textOrNull()

    fun category(row: JsonObject): CategoryDraft? {
        val id = row["category_id"].textOrNull() ?: return null
        val name = row["category_name"].textOrNull() ?: return null
        return CategoryDraft(id, name)
    }

    fun channel(row: JsonObject): ChannelDraft? {
        val id = row["stream_id"].textOrNull() ?: return null
        val name = row["name"].textOrNull() ?: return null
        return ChannelDraft(
            externalId = id,
            number = row["num"].intOrNull(),
            name = name,
            logo = row["stream_icon"].textOrNull(),
            categoryId = row["category_id"].textOrNull(),
            epgId = row["epg_channel_id"].textOrNull(),
            url = null,
        )
    }

    fun movie(row: JsonObject): MovieDraft? {
        val id = row["stream_id"].textOrNull() ?: return null
        val name = row["name"].textOrNull() ?: return null
        return MovieDraft(
            externalId = id,
            name = name,
            poster = row["stream_icon"].textOrNull(),
            categoryId = row["category_id"].textOrNull(),
            year = year(row),
            rating = rating(row),
            containerExtension = row["container_extension"].textOrNull(),
            url = null,
        )
    }

    fun series(row: JsonObject): SeriesDraft? {
        val id = row["series_id"].textOrNull() ?: return null
        val name = row["name"].textOrNull() ?: return null
        return SeriesDraft(
            externalId = id,
            name = name,
            poster = row["cover"].textOrNull(),
            categoryId = row["category_id"].textOrNull(),
            year = year(row),
            rating = rating(row),
            url = null,
        )
    }

    private fun rating(row: JsonObject): Double? {
        val direct = row["rating"].textOrNull()?.toDoubleOrNull()
        if (direct != null && direct > 0) return direct
        val fiveBased = row["rating_5based"].textOrNull()?.toDoubleOrNull()
        if (fiveBased != null && fiveBased > 0) return fiveBased * 2
        return null
    }

    private fun year(row: JsonObject): String? {
        row["year"].textOrNull()?.let { return it }
        val date = row["releasedate"].textOrNull() ?: row["releaseDate"].textOrNull() ?: row["release_date"].textOrNull()
        return date?.let { Regex("^\\d{4}").find(it)?.value }
    }
}

fun JsonElement?.textOrNull(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    val value = primitive.content.trim()
    return value.ifEmpty { null }
}

fun JsonElement?.intOrNull(): Int? {
    val text = textOrNull() ?: return null
    return text.toIntOrNull()
}

fun JsonElement?.positiveDoubleOrNull(): Double? = (this as? JsonPrimitive)?.doubleOrNull?.takeIf { it > 0 }
