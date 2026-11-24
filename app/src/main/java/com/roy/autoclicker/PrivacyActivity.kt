package com.roy.autoclicker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.roy.autoclicker.theme.MaterialPalette
import com.roy.autoclicker.theme.ThemeWindows
import com.roy.autoclicker.widget.Actionbar

class PrivacyActivity : Activity() {

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(newBase)
    ThemeWindows.overrideMode(this, newBase)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    MaterialPalette.applyTheme(this, ClickerSettings(this).paletteId)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_privacy)
    findViewById<Actionbar>(R.id.actionbar).let {
      it.showBackNavigation()
      it.setOnCloseClickListener { finish() }
    }
    ThemeWindows.configureInsets(this, findViewById(R.id.main))
  }
}
