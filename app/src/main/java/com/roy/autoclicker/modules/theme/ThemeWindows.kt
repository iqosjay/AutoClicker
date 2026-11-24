package com.roy.autoclicker.modules.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsets
import com.roy.autoclicker.common.settings.ClickerSettings

object ThemeWindows {

  fun overrideMode(activity: Activity, base: Context): Int {
    val mode = ClickerSettings(base).themeMode
    if (ClickerSettings.THEME_SYSTEM != mode) {
      // 只覆盖夜间模式，保留系统的字号、语言和屏幕配置。
      activity.applyOverrideConfiguration(Configuration().apply {
        this.uiMode = if (ClickerSettings.THEME_DARK == mode) {
          Configuration.UI_MODE_NIGHT_YES
        } else {
          Configuration.UI_MODE_NIGHT_NO
        }
      })
    }
    return mode
  }

  fun configureInsets(activity: Activity, root: View) {
    val window = activity.window
    if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
      window.setDecorFitsSystemWindows(false)
    } else {
      window.decorView.let {
        it.systemUiVisibility = it.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      }
    }
    root.setOnApplyWindowInsetsListener { view, insets ->
      if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
        val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
        view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
      } else {
        view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
      }
      insets
    }
    root.requestApplyInsets()
  }
}
