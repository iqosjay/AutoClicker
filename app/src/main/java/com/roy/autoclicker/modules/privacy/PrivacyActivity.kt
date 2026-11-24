package com.roy.autoclicker.modules.privacy

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import com.roy.autoclicker.R
import com.roy.autoclicker.common.settings.ClickerSettings
import com.roy.autoclicker.common.utils.EmailUtils.setEmailAction
import com.roy.autoclicker.common.widget.Actionbar
import com.roy.autoclicker.modules.theme.MaterialPalette
import com.roy.autoclicker.modules.theme.ThemeWindows

class PrivacyActivity : Activity() {

  private lateinit var textEmail: TextView

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(newBase)
    ThemeWindows.overrideMode(this, newBase)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    MaterialPalette.applyTheme(this, ClickerSettings(this).paletteId)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_privacy)
    textEmail = findViewById(R.id.privacy_contact_email)
    textEmail.let {
      val padding = resources.getDimensionPixelSize(R.dimen.space_small)
      it.minimumHeight = resources.getDimensionPixelSize(R.dimen.touch_target)
      it.isFocusable = true
      it.setPadding(it.paddingLeft, padding, it.paddingRight, padding)
      it.setTextColor(MaterialPalette.resolveColor(this, R.attr.color_action))
      it.setBackgroundResource(R.drawable.background_button)
      it.setEmailAction(getString(R.string.contact_email))
    }
    findViewById<Actionbar>(R.id.actionbar).let {
      it.showBackNavigation()
      it.setOnCloseClickListener { finish() }
    }
    ThemeWindows.configureInsets(this, findViewById(R.id.main))
  }
}
