package com.roy.autoclicker.modules.clicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.roy.autoclicker.R
import kotlin.math.ceil

class TapEngine(private val service: AccessibilityService, private val listener: Listener) {

  interface Listener {
    fun canContinue(): Boolean
    fun onStopped(reason: Int)
    fun onSettled()
  }

  private val handler = Handler(Looper.getMainLooper())
  private val nextTap = Runnable { dispatchNext() }
  private val timeLimit = Runnable { stop(R.string.stopped_time_limit) }
  private val callbackTimeout = Runnable { onCallbackTimeout() }

  private var pending = false
  private var destroyed = false
  private var generation = 0L
  private var deadline = 0L
  private var intervalMillis = 0L
  private var targetX = 0f
  private var targetY = 0f

  var isRunning = false
    private set
  var isFaulted = false
    private set

  val isReady: Boolean
    get() = !destroyed && !isFaulted && !isRunning && !pending

  val remainingSeconds: Long
    get() = ((deadline - SystemClock.uptimeMillis() + 999L) / 1000L).coerceAtLeast(0L)

  fun start(x: Float, y: Float, frequency: Int, durationSeconds: Int): Boolean {
    if (!isReady || !listener.canContinue()) {
      return false
    }
    targetX = x
    targetY = y
    intervalMillis = ceil(1000.0 / frequency).toLong()
    deadline = SystemClock.uptimeMillis() + WINDOW_SETTLE_MILLIS + durationSeconds * 1000L
    ++generation
    isRunning = true
    // 等不可触摸标志提交到窗口系统，避免第一下模拟点击点到圆点自身。
    handler.let {
      it.postDelayed(nextTap, WINDOW_SETTLE_MILLIS)
      it.postAtTime(timeLimit, deadline)
    }
    return true
  }

  fun stop(reason: Int) {
    if (!isRunning) {
      return
    }
    isRunning = false
    ++generation
    handler.let {
      it.removeCallbacks(nextTap)
      it.removeCallbacks(timeLimit)
    }
    listener.onStopped(reason)
    if (!pending) {
      listener.onSettled()
    }
  }

  fun destroy() {
    destroyed = true
    isRunning = false
    ++generation
    handler.removeCallbacksAndMessages(null)
  }

  private fun dispatchNext() {
    if (!isRunning || destroyed || pending) {
      return
    }
    if (!listener.canContinue()) {
      stop(R.string.stopped_environment_changed)
      return
    }
    val startedAt = SystemClock.uptimeMillis()
    if (deadline <= startedAt) {
      stop(R.string.stopped_time_limit)
      return
    }
    val currentGeneration = generation
    val path = Path().also {
      it.moveTo(targetX, targetY)
    }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0L, 1L))
      .build()
    pending = true
    handler.postDelayed(callbackTimeout, CALLBACK_TIMEOUT_MILLIS)
    try {
      val accepted = service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(description: GestureDescription) {
          onGestureResult(currentGeneration, startedAt, true)
        }

        override fun onCancelled(description: GestureDescription) {
          onGestureResult(currentGeneration, startedAt, false)
        }
      }, handler)
      if (!accepted) {
        onDispatchRejected()
      }
    } catch (x: Throwable) {
      Log.w("TapEngine", "Gesture dispatch failed", x)
      // 连接异常时无法确定系统是否已接收手势，不能按普通拒绝处理并允许重试。
      isFaulted = true
      stop(R.string.stopped_dispatch_exception)
    }
  }

  private fun onDispatchRejected() {
    pending = false
    handler.removeCallbacks(callbackTimeout)
    stop(R.string.stopped_dispatch_failed)
  }

  private fun onGestureResult(currentGeneration: Long, startedAt: Long, completed: Boolean) {
    if (destroyed) {
      return
    }
    pending = false
    handler.removeCallbacks(callbackTimeout)
    // 停止后仍可能收到已派发手势的结果；只能恢复定位，不能恢复连点。
    if (currentGeneration != generation || !isRunning) {
      listener.onSettled()
      return
    }
    // 屏幕其他位置的触摸可能取消当前手势，但不能替代用户按下停止按钮。
    // 明确停止仍由上面的 generation 校验拦截，旧回调不会重新启动任务。
    val delay = if (completed) {
      (intervalMillis - (SystemClock.uptimeMillis() - startedAt)).coerceAtLeast(0L)
    } else {
      maxOf(intervalMillis, CANCEL_RETRY_MILLIS)
    }
    handler.postDelayed(nextTap, delay)
  }

  private fun onCallbackTimeout() {
    if (destroyed || !pending) {
      return
    }
    // 未确认手势结束时禁止重启，防止迟到的点击误触“开始”。
    isFaulted = true
    if (isRunning) {
      stop(R.string.stopped_callback_timeout)
    } else {
      listener.onStopped(R.string.stopped_callback_timeout)
    }
  }

  companion object {
    private const val WINDOW_SETTLE_MILLIS = 100L
    private const val CALLBACK_TIMEOUT_MILLIS = 2000L
    private const val CANCEL_RETRY_MILLIS = 32L
  }
}
