package com.roy.autoclicker.modules.theme

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import com.roy.autoclicker.R
import com.roy.autoclicker.common.settings.ClickerSettings
import com.roy.autoclicker.common.widget.Actionbar

class ThemeSettingsActivity : Activity() {

  private lateinit var settings: ClickerSettings
  private lateinit var swatchList: ListView
  private lateinit var swatchAdapter: ThemeSwatchAdapter
  private var selectedMode = ClickerSettings.THEME_SYSTEM
  private var selectedPalette = ClickerSettings.DEFAULT_PALETTE
  private var recreating = false

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(newBase)
    selectedMode = ThemeWindows.overrideMode(this, newBase)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    settings = ClickerSettings(this)
    selectedPalette = MaterialPalette.find(settings.paletteId).id
    MaterialPalette.applyTheme(this, selectedPalette)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_theme_settings)
    findViewById<Actionbar>(R.id.actionbar).let {
      it.showBackNavigation()
      it.setOnCloseClickListener { finish() }
    }

    swatchList = findViewById(R.id.theme_swatches)
    swatchAdapter = ThemeSwatchAdapter(this, selectedPalette) { id -> select(selectedMode, id) }
    val header = layoutInflater.inflate(R.layout.view_theme_options, swatchList, false)
    header.findViewById<Spinner>(R.id.theme_mode).let {
      it.adapter = ThemeModeAdapter(this)
      it.setSelection(selectedMode)
      it.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
          select(position, selectedPalette)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
      }
    }
    header.findViewById<TextView>(R.id.palette_selection).text = getString(
      R.string.palette_selected, MaterialPalette.name(this, selectedPalette)
    )
    swatchList.let {
      // 原生 ListView 统一滚动选项和色块，横屏及大字号时也不会挤掉颜色区域。
      it.itemsCanFocus = true
      it.addHeaderView(header)
      it.adapter = swatchAdapter
      if (null != savedInstanceState) {
        val anchor = savedInstanceState.getInt(STATE_SWATCH_INDEX, -1)
        val position = if (0 > anchor) {
          0
        } else {
          1 + anchor / swatchAdapter.columns
        }
        it.setSelectionFromTop(position, savedInstanceState.getInt(STATE_LIST_TOP))
      }
    }
    ThemeWindows.configureInsets(this, findViewById(R.id.main))
  }

  override fun onSaveInstanceState(outState: Bundle) {
    val row = swatchList.firstVisiblePosition - swatchList.headerViewsCount
    val anchor = if (0 > row) {
      -1
    } else {
      row * swatchAdapter.columns
    }
    outState.putInt(STATE_SWATCH_INDEX, anchor)
    outState.putInt(STATE_LIST_TOP, swatchList.getChildAt(0)?.top ?: 0)
    super.onSaveInstanceState(outState)
  }

  private fun select(mode: Int, paletteId: String) {
    if (recreating || isFinishing || isDestroyed || (mode == selectedMode && paletteId == selectedPalette)) {
      return
    }
    settings.saveTheme(mode, paletteId)
    recreating = true
    // 重新创建主题 Context，让原生控件、弹窗与系统栏使用同一组新颜色。
    recreate()
  }

  companion object {
    private const val STATE_SWATCH_INDEX = "theme_swatch_index"
    private const val STATE_LIST_TOP = "theme_list_top"
  }
}
