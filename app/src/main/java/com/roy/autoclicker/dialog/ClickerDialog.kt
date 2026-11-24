package com.roy.autoclicker.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.roy.autoclicker.ClickerSettings
import com.roy.autoclicker.R
import com.roy.autoclicker.theme.MaterialPalette
import com.roy.autoclicker.utils.ViewUtils.setClickAction
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ClickerDialog private constructor(builder: Builder) : Dialog(builder.activity, R.style.Theme_AutoClicker_Dialog) {

  val screen = builder.screenId
  private val settings = ClickerSettings(builder.activity)
  private val restoredState = builder.dialogState
  private var continuation: Continuation<Int>? = null
  private var resultCode = RESULT_DISMISSED
  private var awaited = false

  private lateinit var titleView: TextView      // 弹窗标题
  private lateinit var contentView: FrameLayout // 对应弹窗的内容容器
  private lateinit var buttonConfirm: Button    // 保存或关闭
  private lateinit var buttonCancel: Button     // 放弃未保存的修改
  private lateinit var inputFrequency: EditText // 目标点击频率
  private lateinit var inputDuration: EditText  // 自动停止时间
  private lateinit var textEmail: TextView      // 联系邮箱入口
  private lateinit var textRepository: TextView // 开源仓库入口

  init {
    MaterialPalette.applyTheme(context, settings.paletteId)
    setOwnerActivity(builder.activity)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_clicker)
    titleView = findViewById(R.id.dialog_title)
    contentView = findViewById(R.id.dialog_content)
    buttonConfirm = findViewById(R.id.dialog_confirm)
    buttonCancel = findViewById(R.id.dialog_cancel)
    val title = when (screen) {
      SCREEN_SETTINGS -> R.string.menu_settings
      SCREEN_HELP     -> R.string.menu_help
      SCREEN_PRIVACY  -> R.string.privacy_title
      else            -> R.string.menu_about
    }
    val layout = when (screen) {
      SCREEN_SETTINGS -> R.layout.dialog_settings
      SCREEN_HELP     -> R.layout.dialog_help
      SCREEN_PRIVACY  -> R.layout.content_privacy
      else            -> R.layout.dialog_about
    }
    titleView.setText(title)
    layoutInflater.inflate(layout, contentView, true)
    buttonCancel.let {
      it.visibility = if (SCREEN_SETTINGS == screen) {
        View.VISIBLE
      } else {
        View.GONE
      }
      it.setClickAction { dismiss() }
    }
    buttonConfirm.let {
      it.setText(when (screen) {
        SCREEN_SETTINGS -> R.string.save_settings
        SCREEN_PRIVACY  -> R.string.privacy_acknowledge
        else            -> R.string.got_it
      })
      it.setClickAction { confirm() }
    }
    if (SCREEN_SETTINGS == screen) {
      configureSettings()
    } else if (SCREEN_ABOUT == screen) {
      configureAbout()
    } else if (SCREEN_PRIVACY == screen) {
      setCanceledOnTouchOutside(false)
    }
  }

  override fun onStart() {
    super.onStart()
    val sideMargin = context.resources.getDimensionPixelSize(R.dimen.space_large) * 2
    val width = minOf(context.resources.displayMetrics.widthPixels - sideMargin, context.resources.getDimensionPixelSize(R.dimen.dialog_max_width))
    window?.let {
      it.setLayout(width.coerceAtLeast(1), WindowManager.LayoutParams.WRAP_CONTENT)
      it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }
  }

  suspend fun await(): Int = suspendCoroutine { pending ->
    check(!awaited)
    awaited = true
    continuation = pending
    try {
      show()
      restoredState?.let { onRestoreInstanceState(it) }
    } catch (x: Throwable) {
      continuation = null
      dismiss()
      pending.resumeWithException(x)
    }
  }

  override fun dismiss() {
    super.dismiss()
    // 页面销毁、返回键和按钮关闭统一结束等待，不把旧 Activity 留在挂起状态。
    val pending = continuation
    continuation = null
    pending?.resume(resultCode)
  }

  override fun cancel() {
    if (SCREEN_PRIVACY == screen && isShowing) {
      // 返回键只关闭告知弹窗，不代表系统授权；销毁时的 dismiss 不记录为已查看。
      settings.markPrivacyPolicySeen()
    }
    super.cancel()
  }

  private fun configureSettings() {
    inputFrequency = findViewById(R.id.input_frequency)
    inputDuration = findViewById(R.id.input_duration)
    inputFrequency.setText(settings.frequency.toString())
    inputDuration.let {
      it.setText(settings.durationSeconds.toString())
      it.setOnEditorActionListener { _, actionId, _ ->
        if (EditorInfo.IME_ACTION_DONE == actionId) {
          confirm()
          true
        } else {
          false
        }
      }
    }
  }

  private fun confirm() {
    if (SCREEN_SETTINGS == screen) {
      if (!saveSettings()) {
        return
      }
      resultCode = RESULT_SAVED
    } else if (SCREEN_PRIVACY == screen) {
      settings.markPrivacyPolicySeen()
      resultCode = RESULT_PRIVACY_SEEN
    }
    dismiss()
  }

  private fun configureAbout() {
    textEmail = findViewById(R.id.contact_email)
    textRepository = findViewById(R.id.github_repository)
    textEmail.let {
      it.paintFlags = it.paintFlags or Paint.UNDERLINE_TEXT_FLAG
      it.setClickAction { openEmailApp() }
    }
    textRepository.let {
      it.paintFlags = it.paintFlags or Paint.UNDERLINE_TEXT_FLAG
      it.setClickAction { openRepository() }
    }
  }

  private fun openRepository() {
    // SSH 克隆地址不能直接由浏览器打开，跳转对应的 HTTPS 页面。
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_repository_url)))
    try {
      context.startActivity(intent)
    } catch (x: Throwable) {
      Log.w("ClickerDialog", "Unable to open repository", x)
      Toast.makeText(context, R.string.browser_unavailable, Toast.LENGTH_LONG).show()
    }
  }

  private fun openEmailApp() {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", context.getString(R.string.contact_email), null))
    try {
      context.startActivity(intent)
    } catch (x: Throwable) {
      Log.w("ClickerDialog", "Unable to open email app", x)
      Toast.makeText(context, R.string.email_app_unavailable, Toast.LENGTH_LONG).show()
    }
  }

  private fun saveSettings(): Boolean {
    val frequency = inputFrequency.text.toString().trim().toIntOrNull()
    val duration = inputDuration.text.toString().trim().toIntOrNull()
    val frequencyValid = null != frequency && frequency in 1..ClickerSettings.MAX_FREQUENCY
    val durationValid = null != duration && duration in 1..ClickerSettings.MAX_DURATION_SECONDS
    inputFrequency.error = if (frequencyValid) {
      null
    } else {
      context.getString(R.string.frequency_invalid)
    }
    inputDuration.error = if (durationValid) {
      null
    } else {
      context.getString(R.string.duration_invalid)
    }
    if (!frequencyValid || !durationValid || null == frequency || null == duration) {
      if (!frequencyValid) {
        inputFrequency.requestFocus()
      } else {
        inputDuration.requestFocus()
      }
      return false
    }
    settings.saveClicking(frequency, duration)
    return true
  }

  class Builder(val activity: Activity) {
    var screenId = SCREEN_SETTINGS
      private set
    var dialogState: Bundle? = null
      private set

    fun setScreen(value: Int): Builder = apply {
      require(value in SCREEN_SETTINGS..SCREEN_PRIVACY)
      this.screenId = value
    }

    fun setSavedState(value: Bundle?): Builder = apply {
      this.dialogState = value
    }

    fun build(): ClickerDialog = ClickerDialog(this)
  }

  companion object {
    const val SCREEN_SETTINGS = 1
    const val SCREEN_HELP = 2
    const val SCREEN_ABOUT = 3
    const val SCREEN_PRIVACY = 4
    const val RESULT_DISMISSED = 0
    const val RESULT_SAVED = 1
    const val RESULT_PRIVACY_SEEN = 2
  }
}
