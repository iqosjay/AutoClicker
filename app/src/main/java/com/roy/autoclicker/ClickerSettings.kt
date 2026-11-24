package com.roy.autoclicker

import android.content.Context

class ClickerSettings(context: Context) {

  private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  val frequency: Int
    get() = preferences.getInt(KEY_FREQUENCY, DEFAULT_FREQUENCY).coerceIn(1, MAX_FREQUENCY)

  val durationSeconds: Int
    get() = preferences.getInt(KEY_DURATION_SECONDS, DEFAULT_DURATION_SECONDS).coerceIn(1, MAX_DURATION_SECONDS)

  val themeMode: Int
    get() = preferences.getInt(KEY_THEME_MODE, THEME_SYSTEM).takeIf { it in THEME_SYSTEM..THEME_DARK } ?: THEME_SYSTEM

  val paletteId: String
    get() = preferences.getString(KEY_PALETTE_ID, DEFAULT_PALETTE) ?: DEFAULT_PALETTE

  fun save(frequency: Int, durationSeconds: Int, themeMode: Int, paletteId: String) {
    preferences.edit()
      .putInt(KEY_FREQUENCY, frequency)
      .putInt(KEY_DURATION_SECONDS, durationSeconds)
      .putInt(KEY_THEME_MODE, themeMode)
      .putString(KEY_PALETTE_ID, paletteId)
      .apply()
  }

  companion object {
    private const val PREFERENCES_NAME = "clicker_settings"
    private const val KEY_FREQUENCY = "frequency"
    private const val KEY_DURATION_SECONDS = "duration_seconds"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_PALETTE_ID = "palette_id"

    const val DEFAULT_PALETTE = "default"
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    const val DEFAULT_FREQUENCY = 100
    const val DEFAULT_DURATION_SECONDS = 60
    const val MAX_FREQUENCY = 100
    const val MAX_DURATION_SECONDS = 3600
  }
}
