package com.hydaui.launcher.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hydaui.launcher.data.UpdateState
import com.hydaui.launcher.data.WhatsNew
import com.hydaui.launcher.ui.components.GlassTone
import com.hydaui.launcher.ui.components.glass
import com.hydaui.launcher.ui.components.pressable
import com.hydaui.launcher.ui.theme.Hyda

private sealed interface Banner {
    data class Permission(val versionName: String) : Banner
    data class Progress(val versionName: String, val progress: Float?) : Banner
    data class Updated(val whatsNew: WhatsNew) : Banner
}

/** Quiet, only-when-needed status for self-updates. Renders nothing most of the time. */
@Composable
fun UpdateBanner(
    update: UpdateState,
    whatsNew: WhatsNew?,
    onAllowInstalls: () -> Unit,
    onDismissWhatsNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val banner: Banner? = when {
        whatsNew != null -> Banner.Updated(whatsNew)
        update is UpdateState.NeedsPermission -> Banner.Permission(update.versionName)
        update is UpdateState.Downloading -> Banner.Progress(update.versionName, update.progress)
        update is UpdateState.Installing -> Banner.Progress(update.versionName, null)
        else -> null
    }
    AnimatedContent(
        targetState = banner,
        contentKey = { it?.javaClass },
        transitionSpec = { fadeIn(spring(stiffness = 300f)) togetherWith fadeOut(spring(stiffness = 300f)) },
        label = "update-banner",
        modifier = modifier,
    ) { b ->
        when (b) {
            null -> Spacer(Modifier.fillMaxWidth())
            is Banner.Permission -> BannerShell(Icons.Rounded.SystemUpdate) {
                Column(Modifier.weight(1f)) {
                    Text("Update ${b.versionName} is ready", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Allow HydaUI to install it — once, and future updates arrive by themselves.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hyda.InkSoft,
                    )
                }
                Spacer(Modifier.width(8.dp))
                PillButton("Allow", onAllowInstalls)
            }

            is Banner.Progress -> BannerShell(Icons.Rounded.SystemUpdate) {
                Column(Modifier.weight(1f)) {
                    Text("Updating to ${b.versionName}…", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    val track = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                    if (b.progress != null) {
                        LinearProgressIndicator(progress = { b.progress }, modifier = track, color = Hyda.Accent, trackColor = Hyda.InkFaint.copy(alpha = 0.2f))
                    } else {
                        LinearProgressIndicator(modifier = track, color = Hyda.Accent, trackColor = Hyda.InkFaint.copy(alpha = 0.2f))
                    }
                }
            }

            is Banner.Updated -> BannerShell(Icons.Rounded.AutoAwesome) {
                Column(Modifier.weight(1f)) {
                    Text("Updated to ${b.whatsNew.versionName}", style = MaterialTheme.typography.labelLarge)
                    if (b.whatsNew.notes.isNotBlank()) {
                        Text(
                            b.whatsNew.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Hyda.InkSoft,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    Modifier
                        .size(30.dp)
                        .pressable(onClick = onDismissWhatsNew)
                        .clip(CircleShape)
                        .background(Hyda.InkFaint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, "Dismiss", tint = Hyda.InkSoft, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BannerShell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .glass(RoundedCornerShape(28.dp), GlassTone.Milk)
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Hyda.Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        content()
    }
}
