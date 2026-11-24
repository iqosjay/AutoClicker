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
    get() = preferences.getString(KEY_PALETTE_ID, SYSTEM_PALETTE)
      ?.takeUnless { LEGACY_DEFAULT_PALETTE == it } ?: SYSTEM_PALETTE

  val hasSeenPrivacyPolicy: Boolean
    get() = PRIVACY_POLICY_VERSION == preferences.getInt(KEY_PRIVACY_POLICY_VERSION, 0)

  fun markPrivacyPolicySeen() {
    preferences.edit().putInt(KEY_PRIVACY_POLICY_VERSION, PRIVACY_POLICY_VERSION).apply()
  }

  fun saveClicking(frequency: Int, durationSeconds: Int) {
    preferences.edit()
      .putInt(KEY_FREQUENCY, frequency)
      .putInt(KEY_DURATION_SECONDS, durationSeconds)
      .apply()
  }

  fun saveTheme(themeMode: Int, paletteId: String) {
    require(themeMode in THEME_SYSTEM..THEME_DARK)
    preferences.edit()
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
    private const val KEY_PRIVACY_POLICY_VERSION = "privacy_policy_version"
    private const val PRIVACY_POLICY_VERSION = 1
    private const val LEGACY_DEFAULT_PALETTE = "default"

    const val SYSTEM_PALETTE = "system"
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    const val DEFAULT_FREQUENCY = 100
    const val DEFAULT_DURATION_SECONDS = 60
    const val MAX_FREQUENCY = 100
    const val MAX_DURATION_SECONDS = 3600
  }
}
