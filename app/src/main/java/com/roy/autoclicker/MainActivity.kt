package com.roy.autoclicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.roy.autoclicker.dialog.ClickerDialog
import com.roy.autoclicker.service.ClickerAccessibilityService
import com.roy.autoclicker.theme.MaterialPalette
import com.roy.autoclicker.utils.ViewUtils.setClickAction
import com.roy.autoclicker.widget.Actionbar
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
  private lateinit var textOverlayPermission: TextView
  private lateinit var textAccessibilityPermission: TextView
  private lateinit var textStatus: TextView
  private lateinit var buttonShow: Button
  private lateinit var buttonStop: Button
  private lateinit var buttonHide: Button
  private var activeDialog: ClickerDialog? = null
  private var popupMenu: PopupMenu? = null
  private var disposing = false
  private var themeModeAtCreation = ClickerSettings.THEME_SYSTEM
  private var paletteAtCreation = ClickerSettings.DEFAULT_PALETTE

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(newBase)
    themeModeAtCreation = ClickerSettings(newBase).themeMode
    if (ClickerSettings.THEME_SYSTEM != themeModeAtCreation) {
      // 只覆盖夜间模式，保留系统的字号、语言和屏幕配置。
      applyOverrideConfiguration(Configuration().apply {
        this.uiMode = if (ClickerSettings.THEME_DARK == themeModeAtCreation) {
          Configuration.UI_MODE_NIGHT_YES
        } else {
          Configuration.UI_MODE_NIGHT_NO
        }
      })
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    settings = ClickerSettings(this)
    paletteAtCreation = MaterialPalette.find(settings.paletteId).id
    MaterialPalette.applyTheme(this, paletteAtCreation)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    actionbar = findViewById(R.id.actionbar)
    textOverlayPermission = findViewById(R.id.text_overlay_permission)
    textAccessibilityPermission = findViewById(R.id.text_accessibility_permission)
    textStatus = findViewById(R.id.text_status)
    buttonShow = findViewById(R.id.button_show)
    buttonStop = findViewById(R.id.button_stop)
    buttonHide = findViewById(R.id.button_hide)

    actionbar.let {
      it.setOnCloseClickListener { finish() }
      it.setOnMenuClickListener { anchor -> showMenu(anchor) }
    }

    findViewById<Button>(R.id.button_overlay_permission).setClickAction {
      ClickerAccessibilityService.hideControls()
      openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
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
    configureInsets()
    savedInstanceState?.let {
      val screen = it.getInt(STATE_DIALOG_SCREEN)
      if (screen in ClickerDialog.SCREEN_SETTINGS..ClickerDialog.SCREEN_ABOUT) {
        showClickerDialog(screen, it.getBundle(STATE_DIALOG_CONTENT))
      }
    }
  }

  override fun onResume() {
    super.onResume()
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
    popupMenu = PopupMenu(ContextThemeWrapper(this, R.style.ThemeOverlay_AutoClicker_Popup), anchor, Gravity.END).apply {
      inflate(R.menu.main_options)
      setOnMenuItemClickListener { item ->
        val screen = when (item.itemId) {
          R.id.menu_settings -> ClickerDialog.SCREEN_SETTINGS
          R.id.menu_help     -> ClickerDialog.SCREEN_HELP
          R.id.menu_about    -> ClickerDialog.SCREEN_ABOUT
          else              -> return@setOnMenuItemClickListener false
        }
        showClickerDialog(screen)
        true
      }
      setOnDismissListener {
        if (popupMenu === it) {
          popupMenu = null
        }
      }
      show()
    }
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
      if (!disposing && !isFinishing && ClickerDialog.RESULT_SAVED == result) {
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        if (settings.themeMode != themeModeAtCreation || MaterialPalette.find(settings.paletteId).id != paletteAtCreation) {
          recreate()
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
          }
        }
      }
    })
  }

  private fun refreshState() {
    textOverlayPermission.setText(when {
      Settings.canDrawOverlays(this) -> R.string.permission_granted
      else                          -> R.string.permission_not_granted
    })
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
    if (!Settings.canDrawOverlays(this)) {
      Toast.makeText(this, R.string.need_overlay_permission, Toast.LENGTH_LONG).show()
      return
    }
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

  private fun configureInsets() {
    if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
      window.setDecorFitsSystemWindows(false)
    } else {
      window.decorView.let {
        it.systemUiVisibility = it.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      }
    }
    findViewById<View>(R.id.main).let { root ->
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

  companion object {
    private const val STATE_DIALOG_SCREEN = "dialog_screen"
    private const val STATE_DIALOG_CONTENT = "dialog_content"
  }
}
