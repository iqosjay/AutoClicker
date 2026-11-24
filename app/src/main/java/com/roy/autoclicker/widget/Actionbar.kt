package com.roy.autoclicker.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import com.roy.autoclicker.R
import com.roy.autoclicker.theme.MaterialPalette
import com.roy.autoclicker.utils.ViewUtils.setClickAction

class Actionbar(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
  constructor(context: Context) : this(context, null)

  private val titleView: TextView
  private val closeButton: ImageButton
  private val menuButton: ImageButton

  var title: CharSequence
    get() = titleView.text
    set(value) {
      titleView.text = value
    }

  init {
    inflate(context, R.layout.view_actionbar, this)
    titleView = findViewById(R.id.actionbar_title)
    closeButton = findViewById(R.id.actionbar_close)
    menuButton = findViewById(R.id.actionbar_menu)
    minimumHeight = resources.getDimensionPixelSize(R.dimen.actionbar_height)
    setBackgroundColor(MaterialPalette.resolveColor(context, R.attr.color_app_bar))
    val attributes = context.obtainStyledAttributes(attrs, R.styleable.Actionbar, defStyleAttr, defStyleRes)
    try {
      title = attributes.getText(R.styleable.Actionbar_actionbar_title) ?: context.getString(R.string.screen_title)
    } finally {
      attributes.recycle()
    }
  }

  fun setOnCloseClickListener(listener: View.OnClickListener) {
    closeButton.setClickAction(listener)
  }

  fun setOnMenuClickListener(listener: View.OnClickListener) {
    menuButton.setClickAction(listener)
  }

  fun showBackNavigation() {
    closeButton.let {
      it.setImageResource(R.drawable.ic_arrow_back)
      it.contentDescription = context.getString(R.string.navigate_back)
    }
    menuButton.visibility = View.GONE
  }
}
