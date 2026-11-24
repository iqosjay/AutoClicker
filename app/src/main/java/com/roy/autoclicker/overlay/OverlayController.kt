package com.roy.autoclicker.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.KeyCharacterMap
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import com.roy.autoclicker.R
import com.roy.autoclicker.utils.ViewUtils.setClickAction
import com.roy.autoclicker.widget.PlaybackDrawable
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class OverlayController(private val context: Context, private val listener: Listener) {

  interface Listener {
    fun onStartRequested(point: PointF)
    fun onStopRequested()
    fun onWindowLost()
    fun onBoundsChanged()
  }

  private val manager = requireNotNull(context.getSystemService(WindowManager::class.java))
  private var targetSize = dp(48)
  private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private val playbackDrawable = PlaybackDrawable(dp(32), Color.WHITE)
  private val targetView = createTargetButton()
  private val targetParams = createParams(targetSize, targetSize)
  private val stopAreas = Array(STOP_AREA_COUNT) { createStopArea() }
  private val stopAreaParams = Array(STOP_AREA_COUNT) { createParams(0, 0) }
  private val safeFrame = Rect()
  private val reportedInsetsFrame = Rect()
  private val targetLocation = IntArray(2)

  private var running = false
  private var targetTouchable = true
  private var stopAreasVisible = false
  private var downX = 0f
  private var downY = 0f
  private var originX = 0
  private var originY = 0
  private var dragged = false
  private var pressAccepted = false
  private var targetEnabledAt = 0L
  private var remainingSeconds = -1L

  var isVisible = false
    private set

  init {
    configureTargetTouch()
    targetView.setOnApplyWindowInsetsListener { _, insets ->
      val frame = readSafeFrame()
      if (isVisible && reportedInsetsFrame != frame) {
        reportedInsetsFrame.set(frame)
        if (safeFrame != frame) {
          listener.onBoundsChanged()
        }
      }
      insets
    }
    targetView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(view: View) {}

      override fun onViewDetachedFromWindow(view: View) {
        playbackDrawable.jumpToCurrentState()
        if (isVisible) {
          listener.onWindowLost()
        }
      }
    })
  }

  fun show(): Boolean {
    if (isVisible) {
      return true
    }
    return try {
      targetSize = dp(48)
      touchSlop = ViewConfiguration.get(context).scaledTouchSlop
      safeFrame.set(readSafeFrame())
      reportedInsetsFrame.set(safeFrame)
      if (targetSize > safeFrame.width() || targetSize > safeFrame.height()) {
        return false
      }
      targetParams.let {
        it.width = targetSize
        it.height = targetSize
        it.x = safeFrame.centerX() - targetSize / 2
        it.y = safeFrame.centerY() - targetSize / 2
        it.flags = BASE_FLAGS
      }
      running = false
      targetTouchable = true
      pressAccepted = false
      targetEnabledAt = SystemClock.uptimeMillis()
      playbackDrawable.setPlaying(false, false)
      targetView.let {
        it.setPadding(dp(8), dp(8), dp(8), dp(8))
        it.contentDescription = context.getString(R.string.overlay_start)
        it.imageAlpha = 255
        it.isClickable = true
      }
      manager.addView(targetView, targetParams)
      isVisible = true
      true
    } catch (x: Throwable) {
      Log.w(TAG, "Unable to attach overlay window", x)
      hide()
      false
    }
  }

  fun hide() {
    isVisible = false
    running = false
    removeStopAreas()
    playbackDrawable.jumpToCurrentState()
    targetView.isPressed = false
    removeView(targetView)
  }

  fun updateDisplayBounds(): Boolean {
    if (!isVisible || running) {
      return false
    }
    return try {
      val horizontalRange = (safeFrame.width() - targetSize).coerceAtLeast(1)
      val verticalRange = (safeFrame.height() - targetSize).coerceAtLeast(1)
      val horizontalFraction = ((targetParams.x - safeFrame.left).toFloat() / horizontalRange).coerceIn(0f, 1f)
      val verticalFraction = ((targetParams.y - safeFrame.top).toFloat() / verticalRange).coerceIn(0f, 1f)
      safeFrame.set(readSafeFrame())
      reportedInsetsFrame.set(safeFrame)
      targetSize = dp(48)
      touchSlop = ViewConfiguration.get(context).scaledTouchSlop
      if (targetSize > safeFrame.width() || targetSize > safeFrame.height()) {
        return false
      }
      // 按可移动区域的相对位置迁移，避免竖屏底部的按钮在横屏时落到屏幕外。
      targetParams.let {
        it.width = targetSize
        it.height = targetSize
        it.x = safeFrame.left + ((safeFrame.width() - targetSize) * horizontalFraction).roundToInt()
        it.y = safeFrame.top + ((safeFrame.height() - targetSize) * verticalFraction).roundToInt()
      }
      pressAccepted = false
      dragged = false
      targetView.let {
        it.isPressed = false
        it.setPadding(dp(8), dp(8), dp(8), dp(8))
      }
      updateTargetLayout()
    } catch (x: Throwable) {
      Log.w(TAG, "Unable to reposition overlay after display change", x)
      false
    }
  }

  fun setRunning(value: Boolean, ready: Boolean): Boolean {
    running = value
    remainingSeconds = -1L
    targetTouchable = !value && ready
    pressAccepted = false
    if (targetTouchable) {
      targetEnabledAt = SystemClock.uptimeMillis()
    }
    playbackDrawable.setPlaying(value)
    targetView.let {
      it.isPressed = false
      it.contentDescription = context.getString(when {
        value -> R.string.overlay_stop
        ready -> R.string.overlay_start
        else  -> R.string.overlay_wait
      })
      it.isClickable = value || ready
      it.imageAlpha = if (value) {
        192
      } else {
        255
      }
    }
    targetParams.flags = if (targetTouchable) {
      BASE_FLAGS
    } else {
      BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    }
    if (!updateTargetLayout()) {
      return false
    }
    if (!value) {
      removeStopAreas()
      return true
    }
    return showStopAreas()
  }

  fun updateRemaining(seconds: Long) {
    if (isVisible && running && seconds != remainingSeconds) {
      remainingSeconds = seconds
      targetView.contentDescription = context.getString(R.string.overlay_stop_description, seconds)
    }
  }

  fun isPointSafe(point: PointF): Boolean {
    return isVisible && targetView.isAttachedToWindow && (!running || stopAreasVisible) &&
      safeFrame.contains(point.x.roundToInt(), point.y.roundToInt())
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun configureTargetTouch() {
    targetView.let { target ->
      // 同一个图标还承接无障碍的停止动作，停止不能被开始时的防抖窗口吞掉。
      target.setClickAction(0L) {
        if (!isVisible) {
          return@setClickAction
        }
        if (running) {
          listener.onStopRequested()
        } else if (targetTouchable) {
          target.getLocationOnScreen(targetLocation)
          listener.onStartRequested(PointF(targetLocation[0] + target.width / 2f, targetLocation[1] + target.height / 2f))
        }
      }
      target.setOnTouchListener { view, event ->
        if (!targetTouchable) {
          return@setOnTouchListener true
        }
        when (event.actionMasked) {
          MotionEvent.ACTION_DOWN         -> {
            // ????????????????????????
            pressAccepted = targetEnabledAt < event.downTime
            if (!pressAccepted) {
              return@setOnTouchListener true
            }
            view.isPressed = true
            downX = event.rawX
            downY = event.rawY
            originX = targetParams.x
            originY = targetParams.y
            dragged = false
          }
          MotionEvent.ACTION_MOVE         -> {
            if (!pressAccepted) {
              return@setOnTouchListener true
            }
            val dx = event.rawX - downX
            val dy = event.rawY - downY
            if (touchSlop < abs(dx) || touchSlop < abs(dy)) {
              dragged = true
            }
            if (dragged) {
              view.isPressed = false
              targetParams.let {
                it.x = (originX + dx.roundToInt()).coerceIn(safeFrame.left, safeFrame.right - targetSize)
                it.y = (originY + dy.roundToInt()).coerceIn(safeFrame.top, safeFrame.bottom - targetSize)
              }
              if (!updateTargetLayout()) {
                listener.onWindowLost()
              }
            }
          }
          MotionEvent.ACTION_UP           -> {
            view.isPressed = false
            if (pressAccepted && !dragged) {
              view.performClick()
            }
            pressAccepted = false
            dragged = false
          }
          MotionEvent.ACTION_POINTER_DOWN -> {
            view.isPressed = false
            dragged = true
          }
          MotionEvent.ACTION_CANCEL       -> {
            view.isPressed = false
            pressAccepted = false
            dragged = true
          }
        }
        true
      }
    }
  }

  private fun showStopAreas(): Boolean {
    if (stopAreasVisible) {
      return true
    }
    targetView.getLocationOnScreen(targetLocation)
    val width = targetView.width
    val height = targetView.height
    // ??????????????????? UI ?????????? 2?3 ???????
    val left = floor(width / 2f).toInt() - TOUCH_HOLE_RADIUS_PIXELS
    val top = floor(height / 2f).toInt() - TOUCH_HOLE_RADIUS_PIXELS
    val right = ceil(width / 2f).toInt() + TOUCH_HOLE_RADIUS_PIXELS
    val bottom = ceil(height / 2f).toInt() + TOUCH_HOLE_RADIUS_PIXELS
    val bounds = arrayOf(
      Rect(0, 0, width, top),
      Rect(0, bottom, width, height),
      Rect(0, top, left, bottom),
      Rect(right, top, width, bottom)
    )
    return try {
      stopAreasVisible = true
      for (index in stopAreas.indices) {
        val area = bounds[index]
        stopAreaParams[index].let {
          it.width = area.width()
          it.height = area.height()
          it.x = targetLocation[0] + area.left
          it.y = targetLocation[1] + area.top
        }
        manager.addView(stopAreas[index], stopAreaParams[index])
      }
      true
    } catch (x: Throwable) {
      Log.w(TAG, "Unable to attach stop touch areas", x)
      removeStopAreas()
      false
    }
  }

  private fun removeStopAreas() {
    stopAreasVisible = false
    for (area in stopAreas) {
      removeView(area)
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun createStopArea(): View {
    return View(context).apply {
      this.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
      setOnTouchListener { _, event ->
        if (isVisible && running && stopAreasVisible && MotionEvent.ACTION_DOWN == event.actionMasked && isPhysicalTouch(event)) {
          targetView.getLocationOnScreen(targetLocation)
          val dx = event.rawX - targetLocation[0] - targetView.width / 2f
          val dy = event.rawY - targetLocation[1] - targetView.height / 2f
          val radius = targetView.width / 2f
          // ????????????????????????
          if (dx * dx + dy * dy <= radius * radius) {
            listener.onStopRequested()
          }
        }
        true
      }
      addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {}

        override fun onViewDetachedFromWindow(view: View) {
          if (isVisible && stopAreasVisible) {
            listener.onWindowLost()
          }
        }
      })
    }
  }

  private fun isPhysicalTouch(event: MotionEvent): Boolean {
    if (0 == event.deviceId || KeyCharacterMap.VIRTUAL_KEYBOARD == event.deviceId) {
      return false
    }
    val device = event.device ?: return false
    return !device.isVirtual
  }

  private fun updateTargetLayout(): Boolean {
    if (!isVisible) {
      return false
    }
    return try {
      manager.updateViewLayout(targetView, targetParams)
      true
    } catch (x: Throwable) {
      Log.w(TAG, "Unable to update target window", x)
      false
    }
  }

  private fun createTargetButton(): ImageView {
    val shape = GradientDrawable().apply {
      this.shape = GradientDrawable.OVAL
      setColor(0xB3000000.toInt())
    }
    return ImageView(context).apply {
      this.scaleType = ImageView.ScaleType.FIT_CENTER
      this.isClickable = true
      this.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
      this.background = RippleDrawable(ColorStateList.valueOf(0x30FFFFFF), shape, null)
      setPadding(dp(8), dp(8), dp(8), dp(8))
      setImageDrawable(playbackDrawable)
    }
  }

  private fun createParams(width: Int, height: Int): WindowManager.LayoutParams {
    return WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, BASE_FLAGS, PixelFormat.TRANSLUCENT).apply {
      this.gravity = Gravity.TOP or Gravity.LEFT
      // 跟随底层应用的方向；图标使用窗口坐标，不额外旋转 View。
      this.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND
      if (Build.VERSION_CODES.P <= Build.VERSION.SDK_INT) {
        this.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
      if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
        setFitInsetsTypes(0)
      }
    }
  }

  private fun readSafeFrame(): Rect {
    if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
      val metrics = manager.currentWindowMetrics
      val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
      return Rect(insets.left, insets.top, metrics.bounds.width() - insets.right, metrics.bounds.height() - insets.bottom)
    }
    val display = manager.defaultDisplay
    val size = Point()
    val availableSize = Point()
    display.let {
      it.getRealSize(size)
      it.getSize(availableSize)
    }
    val cutout = if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
      display.cutout
    } else {
      null
    }
    val cutoutLeft = cutout?.safeInsetLeft ?: 0
    val cutoutTop = cutout?.safeInsetTop ?: 0
    val cutoutRight = cutout?.safeInsetRight ?: 0
    val cutoutBottom = cutout?.safeInsetBottom ?: 0
    val sideNavigation = (size.x - availableSize.x - cutoutLeft - cutoutRight).coerceAtLeast(0)
    val leftNavigation = if (Surface.ROTATION_270 == display.rotation) {
      sideNavigation
    } else {
      0
    }
    // 小悬浮窗的 Insets 相对自身，不代表全屏边界；低版本使用 Display 的全屏可用尺寸。
    // 横屏导航栏可能位于侧面，不能始终从屏幕底部扣除导航栏高度。
    return Rect(
      cutoutLeft + leftNavigation,
      maxOf(systemDimension("status_bar_height"), cutoutTop),
      size.x - cutoutRight - sideNavigation + leftNavigation,
      size.y - maxOf(cutoutBottom, size.y - availableSize.y - cutoutTop)
    )
  }

  private fun systemDimension(name: String): Int {
    val resourceId = context.resources.getIdentifier(name, "dimen", "android")
    return if (0 == resourceId) {
      0
    } else {
      context.resources.getDimensionPixelSize(resourceId)
    }
  }

  private fun removeView(view: View) {
    if (null == view.parent) {
      return
    }
    try {
      manager.removeViewImmediate(view)
    } catch (x: Throwable) {
      Log.w(TAG, "Unable to remove overlay window", x)
    }
  }

  private fun dp(value: Int): Int = (context.resources.displayMetrics.density * value).roundToInt()

  companion object {
    private const val TAG = "OverlayController"
    private const val STOP_AREA_COUNT = 4
    private const val TOUCH_HOLE_RADIUS_PIXELS = 1
    private const val BASE_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
      WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
  }
}
