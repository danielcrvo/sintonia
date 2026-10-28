package com.danielcrvo.sintonia.core.database

import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter

@Database(
    entities = [
        ListEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        FavoriteEntity::class,
        ProgressEntity::class,
        ProgramEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class SintoniaDatabase : RoomDatabase() {
    abstract fun lists(): ListDao
    abstract fun importer(): ImportDao
    abstract fun categories(): CategoryDao
    abstract fun channels(): ChannelDao
    abstract fun movies(): MovieDao
    abstract fun series(): SeriesDao
    abstract fun favorites(): FavoriteDao
    abstract fun progress(): ProgressDao
    abstract fun epg(): EpgDao
}
