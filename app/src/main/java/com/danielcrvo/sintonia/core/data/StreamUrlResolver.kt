package com.danielcrvo.sintonia.core.data

import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.IptvList
import com.danielcrvo.sintonia.core.model.ListType
import com.danielcrvo.sintonia.core.network.Xtream
import com.danielcrvo.sintonia.core.network.XtreamCredentials
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamUrlResolver
    @Inject
    constructor(private val lists: ListsRepository) {
        suspend fun channel(list: IptvList, channel: Channel): String? =
            channel.url ?: xtream(list)?.let { Xtream.playbackUrl(it, "live", channel.externalId, list.liveExtension) }

        suspend fun movie(list: IptvList, externalId: String, directUrl: String?, extension: String?): String? =
            directUrl ?: xtream(list)?.let { Xtream.playbackUrl(it, "movie", externalId, extension ?: DefaultExtension) }

        suspend fun episode(list: IptvList, episodeId: String, extension: String?): String? =
            xtream(list)?.let { Xtream.playbackUrl(it, "series", episodeId, extension ?: DefaultExtension) }

        private suspend fun xtream(list: IptvList): XtreamCredentials? {
            if (list.type != ListType.Xtream) return null
            val password = lists.credentials(list.id).password ?: return null
            return XtreamCredentials(list.server.orEmpty(), list.username.orEmpty(), password)
        }

        private companion object {
            const val DefaultExtension = "mp4"
        }
    }
