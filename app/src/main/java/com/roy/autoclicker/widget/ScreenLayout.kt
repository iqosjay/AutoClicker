package com.roy.autoclicker.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.LinearLayout
import com.roy.autoclicker.R
import com.roy.autoclicker.theme.MaterialPalette

class ScreenLayout(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
  constructor(context: Context) : this(context, null)

  private val headerHeight = resources.getDimensionPixelSize(R.dimen.actionbar_height)
  private val headerPaint = Paint().apply {
    this.color = MaterialPalette.resolveColor(context, android.R.attr.colorPrimary)
  }

  init {
    setWillNotDraw(false)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    // 状态栏在新系统上透明，直接绘制顶部安全区，使其与彩色 Actionbar 连成一体。
    canvas.drawRect(0f, 0f, width.toFloat(), (paddingTop + headerHeight).toFloat(), headerPaint)
  }
}
