package com.hydaui.launcher.ui.drawer

import android.graphics.Rect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.hydaui.launcher.ui.DrawerState
import com.hydaui.launcher.ui.components.pressable
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.hydaui.launcher.data.AppEntry
import com.hydaui.launcher.ui.components.GlassTone
import com.hydaui.launcher.ui.components.glass
import com.hydaui.launcher.ui.theme.Hyda

@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    focusSearch: Boolean,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUninstall: (AppEntry) -> Unit,
    onWebSearch: (String) -> Unit,
    drawer: DrawerState,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val results = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            apps
        } else {
            apps.filter { it.label.contains(q, ignoreCase = true) }
                .sortedBy { !it.label.startsWith(q, ignoreCase = true) }
        }
    }
    // Pulling down from the top of the list hands the finger to the drawer, which follows it
    // down and either springs shut or back open on release.
    val pullToClose = remember(drawer) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag || drawer.progress >= 1f || !drawer.isOpen) return Offset.Zero
                drawer.dragBy(-available.y)
                return Offset(0f, available.y)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag || available.y <= 0f) return Offset.Zero
                drawer.dragBy(-available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (drawer.progress >= 1f) return Velocity.Zero
                drawer.settle(-available.y)
                return available
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF7F8FB).copy(alpha = 0.90f), Color(0xFFE9ECF4).copy(alpha = 0.96f)),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            // Grab handle + header double as a drag-down-to-close zone.
            Column(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(drawer) {
                        val tracker = VelocityTracker()
                        var travelled = 0f
                        detectVerticalDragGestures(
                            onDragStart = {
                                tracker.resetTracking()
                                travelled = 0f
                            },
                            onDragEnd = { drawer.settle(-tracker.calculateVelocity().y) },
                            onDragCancel = { drawer.settle(0f) },
                        ) { change, dy ->
                            travelled += dy
                            // This header moves with the drawer, so track the running total.
                            tracker.addPosition(change.uptimeMillis, Offset(0f, travelled))
                            drawer.dragBy(-dy)
                            change.consume()
                        }
                    }
                    .padding(horizontal = 20.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp)
                        .size(width = 40.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Hyda.InkFaint.copy(alpha = 0.35f)),
                )
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Apps", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${apps.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Hyda.InkFaint,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                SearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    focus = focusSearch,
                    onSubmit = {
                        when {
                            results.isNotEmpty() && query.isNotBlank() -> onLaunch(results.first(), null)
                            query.isNotBlank() -> onWebSearch(query)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.weight(1f).nestedScroll(pullToClose),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = bottomInset + 24.dp),
            ) {
                if (query.isNotBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        WebSearchRow(query, onClick = { onWebSearch(query) })
                    }
                }
                items(results, key = { it.key }) { app ->
                    AppCell(app, onLaunch, onAppInfo, onUninstall)
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, focus: Boolean, onSubmit: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focus) {
        if (focus) runCatching { focusRequester.requestFocus() }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .glass(CircleShape, GlassTone.Milk, elevation = 4.dp)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Hyda.InkFaint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search apps or the web", style = MaterialTheme.typography.bodyLarge, color = Hyda.InkFaint)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Hyda.Ink),
                cursorBrush = SolidColor(Hyda.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                "Clear",
                tint = Hyda.InkFaint,
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun WebSearchRow(query: String, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .fillMaxWidth()
            .pressable(pressedScale = 0.97f, onClick = onClick)
            .glass(RoundedCornerShape(22.dp), elevation = 2.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Public, null, tint = Hyda.Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            "Search the web for “$query”",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(
    app: AppEntry,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUninstall: (AppEntry) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val haptics = LocalHapticFeedback.current
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .pressable(
                    pressedScale = 0.9f,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                ) { onLaunch(app, bounds) }
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(62.dp)
                    .glass(RoundedCornerShape(22.dp), GlassTone.Milk, elevation = 5.dp)
                    .onGloballyPositioned {
                        val r = it.boundsInWindow()
                        bounds = Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(app.icon, contentDescription = null, modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.labelMedium,
                color = Hyda.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color(0xFFF8F9FC),
        ) {
            DropdownMenuItem(
                text = { Text("App info") },
                leadingIcon = { Icon(Icons.Rounded.Info, null) },
                onClick = { menuOpen = false; onAppInfo(app) },
            )
            DropdownMenuItem(
                text = { Text("Uninstall") },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = { menuOpen = false; onUninstall(app) },
            )
        }
    }
}
