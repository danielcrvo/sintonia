package com.danielcrvo.sintonia.core.database

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Content carries the generation it was imported in; reimport writes the next one and flips the pointer last. */
@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val server: String?,
    val username: String?,
    val liveExtension: String = "m3u8",
    val filePath: String?,
    val generation: Int = 0,
    val channelCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val createdAt: Long,
    val lastImportedAt: Long? = null,
)

@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(ListEntity::class, ["id"], ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("listId", "generation", "kind")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val generation: Int,
    val kind: String,
    val externalId: String,
    val name: String,
)

@Entity(
    tableName = "channels",
    foreignKeys = [ForeignKey(ListEntity::class, ["id"], ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index("listId", "generation", "nameKey", "id"),
        Index("listId", "generation", "sortNumber", "nameKey", "id"),
        Index("listId", "generation", "categoryId", "sortNumber", "nameKey", "id"),
        Index("listId", "generation", "categoryId", "nameKey", "id"),
        Index("listId", "externalId"),
        Index("listId", "generation", "epgId"),
    ],
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val generation: Int,
    val externalId: String,
    val number: Int?,
    val sortNumber: Int,
    val name: String,
    val nameKey: String,
    val logo: String?,
    val categoryId: String?,
    val epgId: String?,
    val url: String?,
)

@Entity(
    tableName = "movies",
    foreignKeys = [ForeignKey(ListEntity::class, ["id"], ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index("listId", "generation", "nameKey", "id"),
        Index(
            "listId",
            "generation",
            "sortYear",
            "nameKey",
            "id",
            orders = [Index.Order.ASC, Index.Order.ASC, Index.Order.DESC, Index.Order.ASC, Index.Order.ASC],
        ),
        Index("listId", "generation", "categoryId", "nameKey", "id"),
        Index("listId", "externalId"),
    ],
)
data class MovieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val generation: Int,
    val externalId: String,
    val name: String,
    val nameKey: String,
    val poster: String?,
    val categoryId: String?,
    val year: String?,
    val sortYear: Int,
    val rating: Double?,
    val containerExtension: String?,
    val url: String?,
)

@Entity(
    tableName = "series",
    foreignKeys = [ForeignKey(ListEntity::class, ["id"], ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index("listId", "generation", "nameKey", "id"),
        Index(
            "listId",
            "generation",
            "sortYear",
            "nameKey",
            "id",
            orders = [Index.Order.ASC, Index.Order.ASC, Index.Order.DESC, Index.Order.ASC, Index.Order.ASC],
        ),
        Index("listId", "generation", "categoryId", "nameKey", "id"),
        Index("listId", "externalId"),
    ],
)
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val generation: Int,
    val externalId: String,
    val name: String,
    val nameKey: String,
    val poster: String?,
    val categoryId: String?,
    val year: String?,
    val sortYear: Int,
    val rating: Double?,
    val url: String?,
)

@Entity(
    tableName = "favorites",
    primaryKeys = ["listId", "channelExternalId"],
    foreignKeys = [ForeignKey(ListEntity::class, ["id"], ["listId"], onDelete = ForeignKey.CASCADE)],
)
data class FavoriteEntity(
    val listId: Long,
    val channelExternalId: String,
    val addedAt: Long,
)

@Entity(
    tableName = "progress",
    primaryKeys = ["listId", "kind", "externalId"],
    foreignKeys = [ForeignKey(ListEntity::class, ["id"], ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("listId", "updatedAt")],
)
data class ProgressEntity(
    val listId: Long,
    val kind: String,
    val externalId: String,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAt: Long,
    val title: String?,
    val poster: String?,
    val parentId: String?,
    val season: Int?,
    val episode: Int?,
    val extension: String?,
)

@Entity(
    tableName = "epg_programs",
    foreignKeys = [ForeignKey(ListEntity::class, ["id"], ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("listId", "epgId", "start")],
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val epgId: String,
    val start: Long,
    val stop: Long,
    val title: String,
    val description: String?,
)
