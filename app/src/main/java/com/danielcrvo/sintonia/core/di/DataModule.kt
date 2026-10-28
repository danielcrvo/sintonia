package com.danielcrvo.sintonia.core.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.danielcrvo.sintonia.core.common.KeystoreSecretStore
import com.danielcrvo.sintonia.core.common.SecretStore
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.RoomListsRepository
import com.danielcrvo.sintonia.core.data.importing.CacheImportFiles
import com.danielcrvo.sintonia.core.data.importing.ImportFiles
import com.danielcrvo.sintonia.core.data.importing.ImportStore
import com.danielcrvo.sintonia.core.data.importing.RoomImportStore
import com.danielcrvo.sintonia.core.database.SintoniaDatabase
import com.danielcrvo.sintonia.core.network.Downloader
import com.danielcrvo.sintonia.core.network.HttpClient
import com.danielcrvo.sintonia.core.network.XtreamApi
import com.danielcrvo.sintonia.core.network.XtreamRemote
import com.danielcrvo.sintonia.core.parental.DataStorePinAttempts
import com.danielcrvo.sintonia.core.parental.PinAttemptStore
import com.danielcrvo.sintonia.core.parental.PinClock
import com.danielcrvo.sintonia.core.parental.SystemPinClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProvidesModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): SintoniaDatabase =
        Room
            .databaseBuilder(context, SintoniaDatabase::class.java, "sintonia.db")
            .setDriver(BundledSQLiteDriver())
            .build()

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {
    @Binds abstract fun lists(impl: RoomListsRepository): ListsRepository

    @Binds abstract fun importStore(impl: RoomImportStore): ImportStore

    @Binds abstract fun importFiles(impl: CacheImportFiles): ImportFiles

    @Binds abstract fun secrets(impl: KeystoreSecretStore): SecretStore

    @Binds abstract fun downloader(impl: HttpClient): Downloader

    @Binds abstract fun xtream(impl: XtreamApi): XtreamRemote

    @Binds abstract fun pinAttempts(impl: DataStorePinAttempts): PinAttemptStore

    @Binds abstract fun pinClock(impl: SystemPinClock): PinClock
}
