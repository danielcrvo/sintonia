package com.danielcrvo.sintonia.core.di

import com.danielcrvo.sintonia.core.player.ExoPlayback
import com.danielcrvo.sintonia.core.player.Playback
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {
    @Binds abstract fun playback(impl: ExoPlayback): Playback
}
