package com.hydaui.launcher.ui

import android.util.Log
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
import com.hydaui.launcher.ui.theme.MotionLook
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

    /** Height of the screen the drawer lives in; set from layout. */
    var heightPx by mutableFloatStateOf(1f)

    /** Finger distance that takes the drawer from closed to open. */
    val travelPx: Float get() = (heightPx * MotionLook.drawerTravel).coerceAtLeast(1f)

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

    /**
     * Finger lifted: commit to whichever way it was flung, else to whichever side is nearer.
     *
     * Near either end, position wins unless the fling against it is unmistakable. Velocity
     * estimates go wild when a busy device delivers touches in bursts, and a drawer that's 90%
     * open should never snap shut on a bogus reading.
     */
    fun settle(openingVelocityPx: Float) {
        val p = value.value
        val velocity = (openingVelocityPx / travelPx).coerceIn(-MAX_VELOCITY, MAX_VELOCITY)
        val open = when {
            p >= 0.85f -> velocity > -STRONG_FLING
            p <= 0.15f -> velocity > STRONG_FLING
            velocity > FLING -> true
            velocity < -FLING -> false
            else -> p > 0.5f
        }
        Log.d("HydaDrawer", "settle progress=%.2f velocity=%.2f -> %s".format(p, velocity, if (open) "open" else "close"))
        // Hand the spring the finger's speed only when it agrees with where we're going.
        animateTo(open, if ((velocity > 0) == open) velocity else 0f)
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
                animationSpec = spring(dampingRatio = MotionLook.drawerDamping, stiffness = MotionLook.drawerStiffness),
                initialVelocity = velocity,
            )
        }
    }

    private companion object {
        /** Drawer-lengths per second that count as a deliberate fling. */
        const val FLING = 1.2f

        /** A fling it takes to override where the drawer already nearly is. */
        const val STRONG_FLING = 4f

        /** Anything faster than this is a measuring error, not a thumb. */
        const val MAX_VELOCITY = 12f
    }
}

@Composable
fun rememberDrawerState(): DrawerState {
    val scope = rememberCoroutineScope()
    return remember(scope) { DrawerState(scope) }
}
