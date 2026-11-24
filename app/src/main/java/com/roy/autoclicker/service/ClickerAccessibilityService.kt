package com.roy.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PointF
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.roy.autoclicker.ClickerSettings
import com.roy.autoclicker.R
import com.roy.autoclicker.overlay.OverlayController

class ClickerAccessibilityService : AccessibilityService() {

  private val handler = Handler(Looper.getMainLooper())
  private val consumedVolumeKeys = mutableSetOf<Int>()
  private var engine: TapEngine? = null
  private var overlay: OverlayController? = null
  private var target = PointF()
  private var displaySignature = ""
  private var displayChangePending = false
  private lateinit var settings: ClickerSettings
  private lateinit var powerManager: PowerManager
  private lateinit var keyguardManager: KeyguardManager
  private lateinit var displayManager: DisplayManager

  private val monitorTask = object : Runnable {
    override fun run() {
      val controls = overlay ?: return
      if (!controls.isVisible) {
        return
      }
      if (!isEnvironmentReady()) {
        closeControls(R.string.stopped_environment_changed)
        return
      }
      updateDisplay()
      engine?.let {
        if (it.isRunning) {
          controls.updateRemaining(it.remainingSeconds)
        }
      }
      handler.postDelayed(this, 250L)
    }
  }

  private val displayUpdateTask = Runnable { settleDisplayChange() }

  private val screenReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (Intent.ACTION_SCREEN_OFF == intent.action || Intent.ACTION_SHUTDOWN == intent.action) {
        closeControls(R.string.stopped_screen_off)
      }
    }
  }

  private val displayListener = object : DisplayManager.DisplayListener {
    override fun onDisplayAdded(displayId: Int) {}

    override fun onDisplayRemoved(displayId: Int) {
      if (Display.DEFAULT_DISPLAY == displayId) {
        closeControls(R.string.stopped_window_lost)
      }
    }

    override fun onDisplayChanged(displayId: Int) {
      if (Display.DEFAULT_DISPLAY == displayId) {
        updateDisplay()
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    settings = ClickerSettings(this)
    powerManager = requireNotNull(getSystemService(PowerManager::class.java))
    keyguardManager = requireNotNull(getSystemService(KeyguardManager::class.java))
    displayManager = requireNotNull(getSystemService(DisplayManager::class.java))
    displaySignature = readDisplaySignature()
    displayManager.registerDisplayListener(displayListener, handler)
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_SCREEN_OFF)
      addAction(Intent.ACTION_SHUTDOWN)
    }
    if (Build.VERSION_CODES.TIRAMISU <= Build.VERSION.SDK_INT) {
      registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
      registerReceiver(screenReceiver, filter)
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    releaseSession()
    engine = TapEngine(this, object : TapEngine.Listener {
      override fun canContinue(): Boolean {
        val controls = overlay ?: return false
        return !displayChangePending && displaySignature == readDisplaySignature() &&
          isEnvironmentReady() && controls.isPointSafe(target)
      }

      override fun onStopped(reason: Int) {
        status = getString(reason)
        if (engine?.isFaulted ?: false) {
          overlay?.hide()
          handler.removeCallbacks(monitorTask)
          handler.removeCallbacks(displayUpdateTask)
          displayChangePending = false
        } else {
          updateIdleControls()
        }
      }

      override fun onSettled() {
        updateIdleControls()
      }
    })
    overlay = OverlayController(this, object : OverlayController.Listener {
      override fun onStartRequested(point: PointF) {
        startClicking(point)
      }

      override fun onStopRequested() {
        engine?.stop(R.string.stopped_manually)
      }

      override fun onWindowLost() {
        closeControls(R.string.stopped_window_lost)
      }

      override fun onBoundsChanged() {
        updateDisplay(true)
      }
    })
    instance = this
    status = getString(R.string.status_ready)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

  override fun onInterrupt() {
    closeControls(R.string.stopped_service_interrupted)
  }

  override fun onKeyEvent(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    if (KeyEvent.KEYCODE_VOLUME_UP != keyCode && KeyEvent.KEYCODE_VOLUME_DOWN != keyCode) {
      return false
    }
    if (KeyEvent.ACTION_UP == event.action && consumedVolumeKeys.remove(keyCode)) {
      return true
    }
    if (KeyEvent.ACTION_DOWN == event.action) {
      if (keyCode in consumedVolumeKeys) {
        return true
      }
      if (engine?.isRunning ?: false) {
        // 成对消费按下/抬起，停止后不误调音量；空闲时保持音量键原有用途。
        consumedVolumeKeys.add(keyCode)
        engine?.stop(R.string.stopped_volume_key)
        return true
      }
    }
    return false
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    updateDisplay(displayChangePending)
  }

  override fun onUnbind(intent: Intent): Boolean {
    releaseSession()
    status = getString(R.string.stopped_service_disconnected)
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    releaseSession()
    displayManager.unregisterDisplayListener(displayListener)
    unregisterReceiver(screenReceiver)
    status = getString(R.string.stopped_service_disconnected)
    super.onDestroy()
  }

  private fun startClicking(point: PointF) {
    val tapEngine = engine ?: return
    val controls = overlay ?: return
    updateDisplay()
    if (displayChangePending || !tapEngine.isReady || !isEnvironmentReady()) {
      return
    }
    if (!controls.isPointSafe(point)) {
      Toast.makeText(this, R.string.target_unavailable, Toast.LENGTH_LONG).show()
      return
    }
    target = point
    if (!controls.setRunning(true, false)) {
      closeControls(R.string.stopped_window_lost)
      return
    }
    if (tapEngine.start(point.x, point.y, settings.frequency, settings.durationSeconds)) {
      status = getString(R.string.status_running, settings.frequency)
      controls.updateRemaining(tapEngine.remainingSeconds)
    } else {
      updateIdleControls()
    }
  }

  private fun openControls(): Boolean {
    val tapEngine = engine ?: return false
    val controls = overlay ?: return false
    if (tapEngine.isFaulted) {
      Toast.makeText(this, R.string.service_reset_required, Toast.LENGTH_LONG).show()
      return false
    }
    if (!tapEngine.isReady && !controls.isVisible) {
      Toast.makeText(this, R.string.wait_for_gesture, Toast.LENGTH_SHORT).show()
      return false
    }
    if (!isEnvironmentReady()) {
      status = getString(R.string.stopped_environment_changed)
      return false
    }
    updateDisplay()
    if (!controls.show()) {
      status = getString(R.string.overlay_unavailable)
      Toast.makeText(this, R.string.overlay_unavailable, Toast.LENGTH_LONG).show()
      return false
    }
    displaySignature = readDisplaySignature()
    status = getString(R.string.status_position_target)
    handler.removeCallbacks(monitorTask)
    monitorTask.run()
    return true
  }

  private fun closeControls(reason: Int) {
    val wasVisible = overlay?.isVisible ?: false
    engine?.stop(reason)
    overlay?.hide()
    handler.removeCallbacks(monitorTask)
    handler.removeCallbacks(displayUpdateTask)
    displayChangePending = false
    if (wasVisible) {
      status = getString(reason)
    }
  }

  private fun updateDisplay(boundsChanged: Boolean = false) {
    val signature = readDisplaySignature()
    if (!boundsChanged && signature == displaySignature) {
      return
    }
    displaySignature = signature
    val controls = overlay ?: return
    if (!controls.isVisible) {
      return
    }
    // 先禁止新点击，再停止旧会话；旋转只重新定位窗口，不关闭悬浮入口。
    displayChangePending = true
    engine?.stop(R.string.stopped_display_changed)
    updateIdleControls()
    if (controls.isVisible) {
      handler.let {
        it.removeCallbacks(displayUpdateTask)
        it.postDelayed(displayUpdateTask, DISPLAY_SETTLE_MILLIS)
      }
    }
  }

  private fun settleDisplayChange() {
    val controls = overlay ?: return
    if (!displayChangePending || !controls.isVisible) {
      return
    }
    if (!isEnvironmentReady()) {
      closeControls(R.string.stopped_environment_changed)
      return
    }
    if (readDisplaySignature() != displaySignature) {
      updateDisplay()
      return
    }
    if (!controls.updateDisplayBounds()) {
      closeControls(R.string.stopped_window_lost)
      return
    }
    displayChangePending = false
    // 旧手势尚未结束时仍保持穿透；回调只恢复播放入口，不自动继续连点。
    updateIdleControls()
  }

  private fun updateIdleControls() {
    val tapEngine = engine ?: return
    val controls = overlay ?: return
    if (!controls.isVisible || tapEngine.isRunning) {
      return
    }
    // 尚有手势等待系统回调时，定位按钮继续穿透，避免迟到点击误触开始。
    if (!controls.setRunning(false, !displayChangePending && tapEngine.isReady)) {
      closeControls(R.string.stopped_window_lost)
    }
  }

  private fun isEnvironmentReady(): Boolean {
    return instance === this && Settings.canDrawOverlays(this) && powerManager.isInteractive && !keyguardManager.isKeyguardLocked
  }

  private fun readDisplaySignature(): String {
    val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return ""
    val metrics = DisplayMetrics()
    display.getRealMetrics(metrics)
    return "${display.rotation}:${metrics.widthPixels}:${metrics.heightPixels}:${metrics.densityDpi}"
  }

  private fun releaseSession() {
    if (instance === this) {
      instance = null
    }
    engine?.destroy()
    engine = null
    overlay?.hide()
    overlay = null
    handler.removeCallbacksAndMessages(null)
    displayChangePending = false
    consumedVolumeKeys.clear()
  }

  companion object {
    private const val DISPLAY_SETTLE_MILLIS = 100L
    private var instance: ClickerAccessibilityService? = null
    private var status = ""

    val isConnected: Boolean
      get() = null != instance

    val isClicking: Boolean
      get() = instance?.engine?.isRunning ?: false

    val areControlsVisible: Boolean
      get() = instance?.overlay?.isVisible ?: false

    fun getStatus(context: Context): String = status.ifEmpty { context.getString(R.string.status_idle) }

    fun showControls(): Boolean = instance?.openControls() ?: false

    fun stopClicking(reason: Int) {
      instance?.engine?.stop(reason)
    }

    fun hideControls() {
      instance?.closeControls(R.string.controls_closed)
    }
  }
}
