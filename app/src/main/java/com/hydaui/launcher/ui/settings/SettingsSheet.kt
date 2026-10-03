package com.hydaui.launcher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import com.hydaui.launcher.BuildConfig
import com.hydaui.launcher.data.UpdateState
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.hydaui.launcher.ui.components.GlassTone
import com.hydaui.launcher.ui.components.glass
import com.hydaui.launcher.ui.theme.Hyda

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    name: String,
    useSystemWallpaper: Boolean,
    isDefaultLauncher: Boolean,
    update: UpdateState,
    updatesEnabled: Boolean,
    onCheckForUpdates: () -> Unit,
    onEditName: () -> Unit,
    onUseSystemWallpaperChange: (Boolean) -> Unit,
    onPickWallpaper: () -> Unit,
    onSetDefault: () -> Unit,
    onSystemSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF4F6FA),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("HydaUI", style = MaterialTheme.typography.headlineMedium)
            Text("Make it yours", style = MaterialTheme.typography.headlineSmall, color = Hyda.InkFaint)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), GlassTone.Milk, elevation = 2.dp)) {
                SettingRow(Icons.Rounded.Person, "Your name", name.ifBlank { "Not set" }, onClick = onEditName)
                SettingRow(
                    Icons.Rounded.Wallpaper,
                    "Use system wallpaper",
                    if (useSystemWallpaper) "Showing your wallpaper" else "Showing the HydaUI aurora",
                    onClick = { onUseSystemWallpaperChange(!useSystemWallpaper) },
                ) {
                    Switch(
                        checked = useSystemWallpaper,
                        onCheckedChange = onUseSystemWallpaperChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = Hyda.Accent),
                    )
                }
                if (useSystemWallpaper) {
                    SettingRow(Icons.Rounded.Wallpaper, "Change wallpaper", "Opens the system picker", onClick = onPickWallpaper)
                }
                SettingRow(
                    Icons.Rounded.Home,
                    "Default home app",
                    if (isDefaultLauncher) "HydaUI is your home screen" else "Tap to make HydaUI your home screen",
                    onClick = onSetDefault,
                )
                SettingRow(
                    Icons.Rounded.SystemUpdate,
                    "Updates · ${BuildConfig.VERSION_NAME}",
                    when {
                        !updatesEnabled -> "Preview build — updates are off"
                        update is UpdateState.UpToDate -> "Up to date · tap to check again"
                        update is UpdateState.NeedsPermission -> "${update.versionName} waiting for install permission"
                        update is UpdateState.Downloading -> "Downloading ${update.versionName}…"
                        update is UpdateState.Installing -> "Installing ${update.versionName}…"
                        update is UpdateState.Failed -> "Couldn't check: ${update.reason}"
                        else -> "Tap to check now"
                    },
                    onClick = onCheckForUpdates,
                )
                SettingRow(Icons.Rounded.Settings, "System settings", null, onClick = onSystemSettings)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).glass(CircleShape, elevation = 0.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Hyda.InkSoft, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Hyda.InkFaint)
        }
        trailing?.invoke()
    }
}

@Composable
fun NameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF7F8FB),
        shape = RoundedCornerShape(28.dp),
        title = { Text("What should we call you?") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(24) },
                singleLine = true,
                placeholder = { Text("Your first name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(16.dp),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
