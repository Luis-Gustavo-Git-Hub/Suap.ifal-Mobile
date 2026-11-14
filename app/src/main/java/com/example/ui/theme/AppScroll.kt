package com.example.ui.theme

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Custom fling behavior that caps maximum fling initial velocity
 * to prevent runaway scrolling and frame drops while preserving
 * natural physics.
 */
@Composable
fun rememberAppFlingBehavior(
    maxVelocity: Dp = 4500.dp
): FlingBehavior {
    val density = LocalDensity.current
    val maxVelocityPx = with(density) { maxVelocity.toPx() }
    val defaultFlingBehavior = ScrollableDefaults.flingBehavior()

    return remember(maxVelocityPx, defaultFlingBehavior) {
        AppCappedFlingBehavior(
            delegate = defaultFlingBehavior,
            maxVelocityPx = maxVelocityPx
        )
    }
}

private class AppCappedFlingBehavior(
    private val delegate: FlingBehavior,
    private val maxVelocityPx: Float
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val clampedVelocity = if (initialVelocity.absoluteValue > maxVelocityPx) {
            initialVelocity.sign * maxVelocityPx
        } else {
            initialVelocity
        }
        return with(delegate) {
            performFling(clampedVelocity)
        }
    }
}
