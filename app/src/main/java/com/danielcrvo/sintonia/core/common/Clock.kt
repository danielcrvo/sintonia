package com.danielcrvo.sintonia.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

@Composable
fun rememberNowMillis(periodMillis: Long = 30_000L): State<Long> =
    produceState(initialValue = System.currentTimeMillis(), periodMillis) {
        while (true) {
            delay(periodMillis)
            value = System.currentTimeMillis()
        }
    }
