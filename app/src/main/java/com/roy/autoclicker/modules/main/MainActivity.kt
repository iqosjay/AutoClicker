package com.roy.autoclicker.modules.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import com.roy.autoclicker.R
import com.roy.autoclicker.common.settings.ClickerSettings
import com.roy.autoclicker.common.utils.ViewUtils.setClickAction
import com.roy.autoclicker.common.widget.Actionbar
import com.roy.autoclicker.modules.clicker.ClickerAccessibilityService
import com.roy.autoclicker.modules.privacy.PrivacyActivity
import com.roy.autoclicker.modules.theme.MaterialPalette
import com.roy.autoclicker.modules.theme.ThemeSettingsActivity
import com.roy.autoclicker.modules.theme.ThemeWindows
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class MainActivity : Activity() {

  private val handler = Handler(Looper.getMainLooper())
  private val refreshTask = object : Runnable {
    override fun run() {
      refreshState()
      handler.postDelayed(this, 500L)
    }
  }

  private lateinit var settings: ClickerSettings
  private lateinit var actionbar: Actionbar
  private lateinit var textAccessibilityPermission: TextView
  private lateinit var textStatus: TextView
  private lateinit var buttonShow: Button
  private lateinit var buttonStop: Button
  private lateinit var buttonHide: Button
  private var activeDialog: ClickerDialog? = null
  private var popupMenu: ListPopupWindow? = null
  private var disposing = false
  private var themeModeAtCreation = ClickerSettings.THEME_SYSTEM
  private var paletteAtCreation = ClickerSettings.DEFAULT_PALETTE

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(newBase)
    themeModeAtCreation = ThemeWindows.overrideMode(this, newBase)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    settings = ClickerSettings(this)
    paletteAtCreation = MaterialPalette.find(settings.paletteId).id
    MaterialPalette.applyTheme(this, paletteAtCreation)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    actionbar = findViewById(R.id.actionbar)
    textAccessibilityPermission = findViewById(R.id.text_accessibility_permission)
    textStatus = findViewById(R.id.text_status)
    buttonShow = findViewById(R.id.button_show)
    buttonStop = findViewById(R.id.button_stop)
    buttonHide = findViewById(R.id.button_hide)

    actionbar.let {
      it.setOnCloseClickListener { finish() }
      it.setOnMenuClickListener { anchor -> showMenu(anchor) }
    }

    findViewById<Button>(R.id.button_accessibility).setClickAction {
      ClickerAccessibilityService.hideControls()
      openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    buttonShow.setClickAction {
      showControls()
    }
    buttonStop.setClickAction(0L) {
      ClickerAccessibilityService.stopClicking(R.string.stopped_manually)
      refreshState()
    }
    buttonHide.setClickAction {
      ClickerAccessibilityService.hideControls()
      refreshState()
    }
    ThemeWindows.configureInsets(this, findViewById(R.id.main))
    val screen = savedInstanceState?.getInt(STATE_DIALOG_SCREEN) ?: 0
    val dialogState = savedInstanceState?.getBundle(STATE_DIALOG_CONTENT)
    if (!settings.hasAcceptedPrivacyPolicy) {
      val privacyState = if (ClickerDialog.SCREEN_PRIVACY == screen) {
        dialogState
      } else {
        null
      }
      showClickerDialog(ClickerDialog.SCREEN_PRIVACY, privacyState)
    } else {
      if (screen in ClickerDialog.SCREEN_SETTINGS..ClickerDialog.SCREEN_ABOUT) {
        showClickerDialog(screen, dialogState)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (settings.themeMode != themeModeAtCreation || MaterialPalette.find(settings.paletteId).id != paletteAtCreation) {
      recreate()
      return
    }
    ClickerAccessibilityService.stopClicking(R.string.stopped_app_opened)
    handler.removeCallbacks(refreshTask)
    refreshTask.run()
  }

  override fun onPause() {
    handler.removeCallbacksAndMessages(null)
    popupMenu?.dismiss()
    super.onPause()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    activeDialog?.let {
      if (it.isShowing) {
        outState.putInt(STATE_DIALOG_SCREEN, it.screen)
        outState.putBundle(STATE_DIALOG_CONTENT, it.onSaveInstanceState())
      }
    }
    super.onSaveInstanceState(outState)
  }

  override fun onDestroy() {
    disposing = true
    handler.removeCallbacksAndMessages(null)
    popupMenu?.dismiss()
    popupMenu = null
    activeDialog?.dismiss()
    activeDialog = null
    super.onDestroy()
  }

  private fun showMenu(anchor: View) {
    if (disposing || isFinishing || null != activeDialog) {
      return
    }
    popupMenu?.dismiss()
    val adapter = MainMenuAdapter(this)
    val frame = Rect()
    anchor.getWindowVisibleDisplayFrame(frame)
    val availableWidth = (frame.width() - resources.getDimensionPixelSize(R.dimen.space_medium)).coerceAtLeast(1)
    // 原生 PopupMenu 在 API 29 之前没有公开的强制显示图标接口。
    val menu = ListPopupWindow(this, null, 0, R.style.PopupMenuStyle).apply {
      this.anchorView = anchor
      this.isModal = true
      this.inputMethodMode = ListPopupWindow.INPUT_METHOD_NOT_NEEDED
      setDropDownGravity(Gravity.END)
      setContentWidth(minOf(resources.getDimensionPixelSize(R.dimen.menu_width), availableWidth))
      setListSelector(getDrawable(R.drawable.background_button))
      setAdapter(adapter)
    }
    menu.setOnItemClickListener { _, _, position, _ ->
      menu.dismiss()
      if (!disposing && !isFinishing) {
        when (adapter.getItem(position)) {
          MainMenuAdapter.Item.SETTINGS -> showClickerDialog(ClickerDialog.SCREEN_SETTINGS)
          MainMenuAdapter.Item.THEME    -> startActivity(Intent(this, ThemeSettingsActivity::class.java))
          MainMenuAdapter.Item.HELP     -> showClickerDialog(ClickerDialog.SCREEN_HELP)
          MainMenuAdapter.Item.ABOUT    -> showClickerDialog(ClickerDialog.SCREEN_ABOUT)
          MainMenuAdapter.Item.PRIVACY  -> startActivity(Intent(this, PrivacyActivity::class.java))
        }
      }
    }
    menu.setOnDismissListener {
      if (popupMenu === menu) {
        popupMenu = null
      }
    }
    popupMenu = menu
    menu.show()
  }

  private fun showClickerDialog(screen: Int, savedState: Bundle? = null) {
    if (disposing || isFinishing || null != activeDialog) {
      return
    }
    val dialog = ClickerDialog.Builder(this).setScreen(screen).setSavedState(savedState).build()
    activeDialog = dialog
    // await 只使用 Kotlin 标准库；不为原生工程接入协程框架或生命周期依赖。
    val action: suspend () -> Unit = {
      val result = dialog.await()
      if (activeDialog === dialog) {
        activeDialog = null
      }
      if (!disposing && !isFinishing) {
        when (result) {
          ClickerDialog.RESULT_SAVED        -> Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
          ClickerDialog.RESULT_PRIVACY_EXIT -> exitApp()
          else                             -> {}
        }
      }
    }
    action.startCoroutine(object : Continuation<Unit> {
      override val context = EmptyCoroutineContext

      override fun resumeWith(result: Result<Unit>) {
        result.exceptionOrNull()?.let { x ->
          if (activeDialog === dialog) {
            activeDialog = null
          }
          Log.w("MainActivity", "Unable to show dialog", x)
          if (!disposing && !isFinishing) {
            Toast.makeText(this@MainActivity, R.string.dialog_unavailable, Toast.LENGTH_SHORT).show()
            if (ClickerDialog.SCREEN_PRIVACY == screen) {
              exitApp()
            }
          }
        }
      }
    })
  }

  private fun exitApp() {
    ClickerAccessibilityService.hideControls()
    finishAndRemoveTask()
  }

  private fun refreshState() {
    textAccessibilityPermission.setText(when {
      ClickerAccessibilityService.isConnected -> R.string.service_connected
      else                                   -> R.string.service_not_connected
    })
    textStatus.text = ClickerAccessibilityService.getStatus(this)
    buttonShow.setText(when {
      ClickerAccessibilityService.areControlsVisible -> R.string.return_to_controls
      else                                          -> R.string.show_controls
    })
    buttonStop.isEnabled = ClickerAccessibilityService.isClicking
    buttonHide.isEnabled = ClickerAccessibilityService.areControlsVisible
  }

  private fun showControls() {
    if (!ClickerAccessibilityService.isConnected) {
      Toast.makeText(this, R.string.need_accessibility, Toast.LENGTH_LONG).show()
      return
    }
    if (ClickerAccessibilityService.showControls()) {
      moveTaskToBack(true)
    }
  }

  private fun openSettings(intent: Intent) {
    try {
      startActivity(intent)
    } catch (x: Throwable) {
      Log.w("MainActivity", "Unable to open permission settings", x)
      Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_LONG).show()
    }
  }

  companion object {
    private const val STATE_DIALOG_SCREEN = "dialog_screen"
    private const val STATE_DIALOG_CONTENT = "dialog_content"
  }
}
