package com.danielcrvo.sintonia.navigation

import androidx.navigation3.runtime.NavKey
import com.danielcrvo.sintonia.core.model.PlayerRequest
import kotlinx.serialization.Serializable

@Serializable
data object Welcome : NavKey

@Serializable
data object AddList : NavKey

@Serializable
data class Importing(val listId: Long, val isNewList: Boolean) : NavKey

@Serializable
data class MovieDetail(val movieId: Long) : NavKey

@Serializable
data class SeriesDetail(val seriesId: Long) : NavKey

@Serializable
data object Search : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object Lists : NavKey

@Serializable
data object Parental : NavKey

@Serializable
data object Categories : NavKey

@Serializable
data class Player(val request: PlayerRequest) : NavKey

@Serializable
data object Main : NavKey
