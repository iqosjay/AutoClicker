package com.roy.autoclicker.modules.theme

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.roy.autoclicker.R

class ThemeModeAdapter(private val context: Context) : BaseAdapter() {

  private val labels = intArrayOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)

  override fun getCount(): Int = labels.size

  override fun getItem(position: Int): String = context.getString(labels[position])

  override fun getItemId(position: Int): Long = position.toLong()

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = bind(position, convertView)

  override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = bind(position, convertView)

  private fun bind(position: Int, convertView: View?): TextView {
    val view = convertView as? TextView ?: TextView(context).apply {
      this.minHeight = resources.getDimensionPixelSize(R.dimen.touch_target)
      this.gravity = Gravity.CENTER_VERTICAL
      this.textSize = 16f
      val horizontal = resources.getDimensionPixelSize(R.dimen.space_medium)
      val vertical = resources.getDimensionPixelSize(R.dimen.space_small)
      setPadding(horizontal, vertical, horizontal, vertical)
      setTextColor(context.getColor(R.color.text_primary))
    }
    return view.apply { this.text = getItem(position) }
  }
}
