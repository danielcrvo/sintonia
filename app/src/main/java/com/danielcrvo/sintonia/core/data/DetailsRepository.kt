package com.danielcrvo.sintonia.core.data

import com.danielcrvo.sintonia.core.model.IptvList
import com.danielcrvo.sintonia.core.model.ListType
import com.danielcrvo.sintonia.core.model.SeriesInfo
import com.danielcrvo.sintonia.core.model.VodInfo
import com.danielcrvo.sintonia.core.network.XtreamApi
import com.danielcrvo.sintonia.core.network.XtreamCredentials
import com.danielcrvo.sintonia.core.network.hostOf
import com.danielcrvo.sintonia.core.network.toApiException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface DetailsRepository {
    suspend fun movieInfo(list: IptvList, externalId: String): VodInfo?
    suspend fun seriesInfo(list: IptvList, externalId: String): SeriesInfo?
}

@Singleton
class XtreamDetailsRepository
    @Inject
    constructor(
        private val api: XtreamApi,
        private val lists: ListsRepository,
    ) : DetailsRepository {
        private val movies = ConcurrentHashMap<String, VodInfo>()
        private val series = ConcurrentHashMap<String, SeriesInfo>()

        override suspend fun movieInfo(list: IptvList, externalId: String): VodInfo? {
            val creds = credentials(list) ?: return null
            val key = "${list.id}:$externalId"
            movies[key]?.let { return it }
            return try {
                api.vodInfo(creds, externalId).also { movies[key] = it }
            } catch (error: Throwable) {
                throw error.toApiException(hostOf(list.server))
            }
        }

        override suspend fun seriesInfo(list: IptvList, externalId: String): SeriesInfo? {
            val creds = credentials(list) ?: return null
            val key = "${list.id}:$externalId"
            series[key]?.let { return it }
            return try {
                api.seriesInfo(creds, externalId).also { series[key] = it }
            } catch (error: Throwable) {
                throw error.toApiException(hostOf(list.server))
            }
        }

        private suspend fun credentials(list: IptvList): XtreamCredentials? {
            if (list.type != ListType.Xtream) return null
            val password = lists.credentials(list.id).password ?: return null
            return XtreamCredentials(list.server.orEmpty(), list.username.orEmpty(), password)
        }
    }
