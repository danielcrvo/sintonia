package com.danielcrvo.sintonia.core.di

import com.danielcrvo.sintonia.core.data.DataStoreParentalRepository
import com.danielcrvo.sintonia.core.data.DetailsRepository
import com.danielcrvo.sintonia.core.data.LiveRepository
import com.danielcrvo.sintonia.core.data.MovieRepository
import com.danielcrvo.sintonia.core.data.ParentalRepository
import com.danielcrvo.sintonia.core.data.ProgressRepository
import com.danielcrvo.sintonia.core.data.RoomLiveRepository
import com.danielcrvo.sintonia.core.data.RoomMovieRepository
import com.danielcrvo.sintonia.core.data.RoomProgressRepository
import com.danielcrvo.sintonia.core.data.RoomSearchRepository
import com.danielcrvo.sintonia.core.data.RoomSeriesRepository
import com.danielcrvo.sintonia.core.data.SearchRepository
import com.danielcrvo.sintonia.core.data.SeriesRepository
import com.danielcrvo.sintonia.core.data.XtreamDetailsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun live(impl: RoomLiveRepository): LiveRepository

    @Binds abstract fun parental(impl: DataStoreParentalRepository): ParentalRepository

    @Binds abstract fun movies(impl: RoomMovieRepository): MovieRepository

    @Binds abstract fun series(impl: RoomSeriesRepository): SeriesRepository

    @Binds abstract fun progress(impl: RoomProgressRepository): ProgressRepository

    @Binds abstract fun search(impl: RoomSearchRepository): SearchRepository

    @Binds abstract fun details(impl: XtreamDetailsRepository): DetailsRepository
}
