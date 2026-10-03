package com.hydaui.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Click handling that answers the press with the glass itself: it sinks a little under the
 * finger and springs back on release, instead of a ripple washing over translucent surfaces.
 *
 * Put it *before* [glass] in the chain so the whole pane scales.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    onLongClick: (() -> Unit)? = null,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) {
            spring(stiffness = 1_400f)
        } else {
            spring(dampingRatio = 0.45f, stiffness = 520f)
        },
        label = "press",
    )
    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}
