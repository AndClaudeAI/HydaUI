package com.hydaui.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * One number, 0 (closed) to 1 (open), shared by every gesture and animation that touches the
 * app drawer, so the drawer, the home screen behind it and the finger never disagree.
 *
 * Drags move it 1:1 with the finger over [travelPx]; releases hand the finger's velocity to a
 * spring so the motion carries on rather than restarting.
 */
@Stable
class DrawerState internal constructor(private val scope: CoroutineScope) {
    private val value = Animatable(0f)

    /** Finger distance that takes the drawer from closed to open. */
    var travelPx by mutableFloatStateOf(1f)

    /** Where the drawer is right now. May overshoot [0, 1] slightly on a spring; clamp when drawing. */
    val progress: Float get() = value.value

    /** Where the drawer is headed (or resting). */
    var isOpen by mutableStateOf(false)
        private set

    /** Moves the drawer with the finger. Positive [openingPx] opens it. */
    fun dragBy(openingPx: Float) {
        val target = (value.value + openingPx / travelPx).coerceIn(0f, 1f)
        // Undispatched so the frame that delivered the touch also draws its result.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { value.snapTo(target) }
    }

    /** Finger lifted: commit to whichever way it was flung, else to whichever side is nearer. */
    fun settle(openingVelocityPx: Float) {
        val velocity = openingVelocityPx / travelPx
        val open = when {
            velocity > FLING -> true
            velocity < -FLING -> false
            else -> value.value > 0.5f
        }
        animateTo(open, velocity)
    }

    fun open() = animateTo(true, 0f)

    fun close() = animateTo(false, 0f)

    /** Closes without animating — for when the drawer is already off-screen behind another app. */
    fun snapClosed() {
        isOpen = false
        scope.launch { value.snapTo(0f) }
    }

    private fun animateTo(open: Boolean, velocity: Float) {
        isOpen = open
        scope.launch {
            value.animateTo(
                targetValue = if (open) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.88f, stiffness = 320f),
                initialVelocity = velocity,
            )
        }
    }

    private companion object {
        /** Drawer-lengths per second that count as a deliberate fling. */
        const val FLING = 1.2f
    }
}

@Composable
fun rememberDrawerState(): DrawerState {
    val scope = rememberCoroutineScope()
    return remember(scope) { DrawerState(scope) }
}
