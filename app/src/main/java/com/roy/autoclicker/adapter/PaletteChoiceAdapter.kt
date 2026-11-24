package com.roy.autoclicker.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.roy.autoclicker.R

data class PaletteChoice(val id: String, val label: String, val color: Int)

class PaletteChoiceAdapter(context: Context, private val entries: List<PaletteChoice>) : BaseAdapter() {

  private val inflater = LayoutInflater.from(context)
  private val cornerRadius = context.resources.getDimension(R.dimen.corner_button)

  override fun getCount(): Int = entries.size

  override fun getItem(position: Int): PaletteChoice = entries[position]

  override fun getItemId(position: Int): Long = position.toLong()

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = createRow(position, convertView, parent, false)

  // SDK 的重写签名固定，自定义命名统一使用 dropdown。
  override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = createRow(position, convertView, parent, true)

  private fun createRow(position: Int, convertView: View?, parent: ViewGroup, dropdown: Boolean): View {
    val row = convertView ?: inflater.inflate(R.layout.item_palette_choice, parent, false).apply {
      this.tag = Holder(this)
    }
    val holder = row.tag as Holder
    val item = getItem(position)
    holder.let {
      it.label.text = item.label
      it.swatch.background = GradientDrawable().apply {
        this.cornerRadius = this@PaletteChoiceAdapter.cornerRadius
        setColor(item.color)
      }
      it.arrow.visibility = if (dropdown) {
        View.GONE
      } else {
        View.VISIBLE
      }
    }
    return row
  }

  private class Holder(view: View) {
    val swatch: View = view.findViewById(R.id.palette_swatch)
    val label: TextView = view.findViewById(R.id.palette_choice_label)
    val arrow: ImageView = view.findViewById(R.id.palette_choice_arrow)
  }
}
