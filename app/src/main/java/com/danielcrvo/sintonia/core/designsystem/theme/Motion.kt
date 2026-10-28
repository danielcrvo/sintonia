package com.danielcrvo.sintonia.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

@Immutable
class Motion(private val reduced: Boolean) {
    fun <T> fast(): FiniteAnimationSpec<T> = if (reduced) snap() else tween(FastMillis, easing = Standard)
    fun <T> normal(): FiniteAnimationSpec<T> = if (reduced) snap() else tween(NormalMillis, easing = Standard)
    fun <T> slow(): FiniteAnimationSpec<T> = if (reduced) snap() else tween(SlowMillis, easing = Standard)

    val isReduced: Boolean get() = reduced

    companion object {
        const val FastMillis = 120
        const val NormalMillis = 200
        const val SlowMillis = 300
        val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    }
}
