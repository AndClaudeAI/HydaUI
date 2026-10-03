package com.hydaui.launcher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("hyda", Context.MODE_PRIVATE)

    private val _name = MutableStateFlow(sp.getString(KEY_NAME, "").orEmpty())
    val name: StateFlow<String> = _name.asStateFlow()

    private val _useSystemWallpaper = MutableStateFlow(sp.getBoolean(KEY_WALLPAPER, false))
    val useSystemWallpaper: StateFlow<Boolean> = _useSystemWallpaper.asStateFlow()

    fun setName(value: String) {
        val trimmed = value.trim()
        sp.edit().putString(KEY_NAME, trimmed).apply()
        _name.value = trimmed
    }

    fun setUseSystemWallpaper(value: Boolean) {
        sp.edit().putBoolean(KEY_WALLPAPER, value).apply()
        _useSystemWallpaper.value = value
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_WALLPAPER = "use_system_wallpaper"
    }
}
