package com.danielcrvo.sintonia.core.data.importing

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RefresherEntryPoint {
    fun refresher(): ListRefresher
}

class GuideRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val refresher = EntryPointAccessors.fromApplication(applicationContext, RefresherEntryPoint::class.java).refresher()
        return if (refresher.refreshIfStale()) Result.success() else Result.retry()
    }

    companion object {
        private const val Name = "list-refresh"
        private const val IntervalHours = 6L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GuideRefreshWorker>(IntervalHours, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(Name, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
