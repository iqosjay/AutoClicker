package com.roy.autoclicker.modules.clicker

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.animation.PathInterpolator

class PlaybackDrawable(private val sizePixels: Int, colorIcon: Int) : Drawable() {

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = colorIcon
    this.style = Paint.Style.FILL
  }
  private val path = Path()
  // 三角形尖端拆成两个重合顶点，与正方形保持一致的路径结构。
  private val pointsPlay = floatArrayOf(8f, 5f, 19f, 12f, 19f, 12f, 8f, 19f)
  private val pointsStop = floatArrayOf(6f, 6f, 18f, 6f, 18f, 18f, 6f, 18f)
  private var progress = 0f
  private var destination = 0f
  private var animator: ValueAnimator? = null

  fun setPlaying(playing: Boolean, animate: Boolean = true) {
    destination = if (playing) {
      1f
    } else {
      0f
    }
    cancelAnimation()
    val animationsEnabled = Build.VERSION_CODES.O > Build.VERSION.SDK_INT || ValueAnimator.areAnimatorsEnabled()
    if (!animate || !animationsEnabled || null == callback || destination == progress) {
      progress = destination
      invalidateSelf()
      return
    }
    // 从当前形状接着变形，快速切换时不会跳回动画起点。
    animator = ValueAnimator.ofFloat(progress, destination).apply {
      this.duration = 240L
      this.interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
      addUpdateListener {
        progress = it.animatedValue as Float
        invalidateSelf()
      }
      start()
    }
  }

  override fun draw(canvas: Canvas) {
    val size = minOf(bounds.width(), bounds.height()).toFloat()
    if (0f >= size) {
      return
    }
    val saved = canvas.save()
    canvas.let {
      it.translate(bounds.left + (bounds.width() - size) / 2f, bounds.top + (bounds.height() - size) / 2f)
      it.scale(size / 24f, size / 24f)
      // 共用变形进度，快速切换时形状和旋转角度都能连续反向。
      it.rotate(180f * progress, 12f, 12f)
    }
    path.rewind()
    for (index in 0 until 4) {
      val offset = index * 2
      val x = pointsPlay[offset] + (pointsStop[offset] - pointsPlay[offset]) * progress
      val y = pointsPlay[offset + 1] + (pointsStop[offset + 1] - pointsPlay[offset + 1]) * progress
      if (0 == index) {
        path.moveTo(x, y)
      } else {
        path.lineTo(x, y)
      }
    }
    path.close()
    canvas.let {
      it.drawPath(path, paint)
      it.restoreToCount(saved)
    }
  }

  override fun setAlpha(alpha: Int) {
    paint.alpha = alpha
    invalidateSelf()
  }

  override fun getAlpha(): Int = paint.alpha

  override fun setColorFilter(colorFilter: ColorFilter?) {
    paint.colorFilter = colorFilter
    invalidateSelf()
  }

  override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

  override fun getIntrinsicWidth(): Int = sizePixels

  override fun getIntrinsicHeight(): Int = sizePixels

  override fun jumpToCurrentState() {
    cancelAnimation()
    progress = destination
    invalidateSelf()
  }

  private fun cancelAnimation() {
    animator?.let {
      it.removeAllUpdateListeners()
      it.cancel()
    }
    animator = null
  }
}
