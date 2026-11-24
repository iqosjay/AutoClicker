package com.roy.autoclicker.theme

import android.content.Context
import android.graphics.Color
import com.roy.autoclicker.ClickerSettings
import com.roy.autoclicker.R

data class MaterialSwatch(val id: String, val tone: String, val colorResource: Int, val styleResource: Int)

data class MaterialFamily(val id: String, val nameResource: Int, val swatches: List<MaterialSwatch>)

object MaterialPalette {

  private val defaultSwatch = MaterialSwatch(ClickerSettings.DEFAULT_PALETTE, "", R.color.color_accent, 0)
  val families = listOf(MaterialFamily(ClickerSettings.DEFAULT_PALETTE, R.string.palette_default, listOf(defaultSwatch))) + GENERATED_MATERIAL_FAMILIES
  private val swatches = families.flatMap { it.swatches }.associateBy { it.id }

  fun find(id: String): MaterialSwatch = swatches[id] ?: defaultSwatch

  fun familyIndex(id: String): Int = families.indexOfFirst { family -> family.swatches.any { it.id == id } }.coerceAtLeast(0)

  fun applyTheme(context: Context, id: String) {
    val style = find(id).styleResource
    if (0 != style) {
      context.theme.applyStyle(style, true)
    }
  }

  fun resolveColor(context: Context, attribute: Int): Int {
    val attributes = context.obtainStyledAttributes(intArrayOf(attribute))
    return try {
      attributes.getColor(0, context.getColor(R.color.text_primary))
    } finally {
      attributes.recycle()
    }
  }

  fun textColor(background: Int): Int {
    val luminance = Color.luminance(background).toDouble()
    return if ((luminance + 0.05) / 0.05 >= 1.05 / (luminance + 0.05)) {
      Color.BLACK
    } else {
      Color.WHITE
    }
  }
}
