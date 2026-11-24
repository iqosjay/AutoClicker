package com.roy.autoclicker.modules.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Checkable
import android.widget.RadioButton
import com.roy.autoclicker.R

class ThemeSwatchButton(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : View(context, attrs, defStyleAttr, defStyleRes), Checkable {

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
  constructor(context: Context) : this(context, null)

  private var checkedState = false
  private val fill = GradientDrawable().apply { this.shape = GradientDrawable.OVAL }
  private val checkIcon = requireNotNull(context.getDrawable(R.drawable.ic_check)).mutate()

  init {
    isClickable = true
    isFocusable = true
    isSaveEnabled = false
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
  }

  fun bind(color: Int, selected: Boolean, description: String) {
    val foreground = MaterialPalette.textColor(color)
    fill.let {
      it.setColor(color)
      it.setStroke(resources.displayMetrics.density.toInt().coerceAtLeast(1), (foreground and 0xFFFFFF) or 0x40000000)
    }
    val mask = GradientDrawable().apply {
      this.shape = GradientDrawable.OVAL
      setColor(Color.WHITE)
    }
    background = RippleDrawable(ColorStateList.valueOf((foreground and 0xFFFFFF) or 0x30000000), fill, mask)
    checkIcon.setTint(foreground)
    contentDescription = description
    if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
      tooltipText = description
    }
    setChecked(selected)
  }

  override fun isChecked(): Boolean = checkedState

  override fun setChecked(value: Boolean) {
    checkedState = value
    isSelected = value
    refreshDrawableState()
    invalidate()
  }

  override fun toggle() {
    setChecked(!checkedState)
  }

  override fun onCreateDrawableState(extraSpace: Int): IntArray {
    val state = super.onCreateDrawableState(extraSpace + 1)
    if (checkedState) {
      mergeDrawableStates(state, intArrayOf(android.R.attr.state_checked))
    }
    return state
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (checkedState) {
      val iconSize = resources.getDimensionPixelSize(R.dimen.icon_size)
      val left = (width - iconSize) / 2
      val top = (height - iconSize) / 2
      checkIcon.let {
        it.setBounds(left, top, left + iconSize, top + iconSize)
        it.draw(canvas)
      }
    }
  }

  override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
    super.onInitializeAccessibilityNodeInfo(info)
    info.let {
      it.className = RadioButton::class.java.name
      it.isCheckable = true
      it.isChecked = checkedState
    }
  }
}
