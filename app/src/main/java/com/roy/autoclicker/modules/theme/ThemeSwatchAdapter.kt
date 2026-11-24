package com.roy.autoclicker.modules.theme

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.roy.autoclicker.R
import com.roy.autoclicker.common.utils.ViewUtils.setClickAction
import java.util.Locale

class ThemeSwatchAdapter(
  private val context: Context,
  private val selectedId: String,
  private val onSelect: (String) -> Unit
) : BaseAdapter() {

  private val swatches = MaterialPalette.swatches
  private val size = context.resources.getDimensionPixelSize(R.dimen.theme_swatch_size)
  private val spacing = context.resources.getDimensionPixelSize(R.dimen.space_small)
  val columns = context.resources.getInteger(R.integer.theme_swatch_columns)

  override fun getCount(): Int = (swatches.size + columns - 1) / columns

  override fun getItem(position: Int) = swatches.subList(position * columns, minOf((position + 1) * columns, swatches.size))

  override fun getItemId(position: Int): Long = position.toLong()

  override fun hasStableIds(): Boolean = true

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val row = convertView as? LinearLayout ?: createRow()
    val items = getItem(position)
    for (column in 0 until columns) {
      val button = (row.getChildAt(column) as FrameLayout).getChildAt(0) as ThemeSwatchButton
      val swatch = items.getOrNull(column)
      button.let {
        it.visibility = if (null == swatch) {
          View.INVISIBLE
        } else {
          View.VISIBLE
        }
        if (null != swatch) {
          val color = context.getColor(swatch.colorResource)
          val name = MaterialPalette.name(context, swatch.id)
          val hex = String.format(Locale.ROOT, "#%06X", color and 0xFFFFFF)
          it.bind(color, swatch.id == selectedId, context.getString(R.string.palette_swatch_description, name, hex))
          it.setClickAction { onSelect(swatch.id) }
        }
      }
    }
    return row
  }

  private fun createRow(): LinearLayout {
    return LinearLayout(context).apply {
      this.orientation = LinearLayout.HORIZONTAL
      this.layoutParams = android.widget.AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size + spacing * 2)
      this.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
      setPadding(spacing, spacing, spacing, spacing)
      repeat(columns) {
        val cell = FrameLayout(context).apply {
          this.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
          addView(ThemeSwatchButton(context), FrameLayout.LayoutParams(size, size, Gravity.CENTER))
        }
        addView(cell)
      }
    }
  }
}
