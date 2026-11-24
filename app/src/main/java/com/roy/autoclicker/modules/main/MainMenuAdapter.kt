package com.roy.autoclicker.modules.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.roy.autoclicker.R

class MainMenuAdapter(context: Context) : BaseAdapter() {

  private val inflater = LayoutInflater.from(context)
  private val items = Item.entries

  override fun getCount(): Int = items.size

  override fun getItem(position: Int): Item = items[position]

  override fun getItemId(position: Int): Long = getItem(position).ordinal.toLong()

  override fun hasStableIds(): Boolean = true

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val view = convertView as? TextView ?: inflater.inflate(R.layout.item_main_menu, parent, false) as TextView
    val item = getItem(position)
    return view.apply {
      setText(item.titleResource)
      setCompoundDrawablesRelativeWithIntrinsicBounds(item.iconResource, 0, 0, 0)
    }
  }

  enum class Item(val titleResource: Int, val iconResource: Int) {
    SETTINGS(R.string.menu_settings, R.drawable.ic_settings),
    THEME(R.string.theme_settings_title, R.drawable.ic_palette),
    HELP(R.string.menu_help, R.drawable.ic_help_outline),
    ABOUT(R.string.menu_about, R.drawable.ic_info),
    PRIVACY(R.string.privacy_title, R.drawable.ic_privacy_tip)
  }
}
