package com.roy.autoclicker.theme

import android.content.Context
import android.graphics.Color
import com.roy.autoclicker.ClickerSettings
import com.roy.autoclicker.R

data class MaterialSwatch(val id: String, val tone: String, val colorResource: Int, val styleResource: Int)

data class MaterialFamily(val id: String, val nameResource: Int, val swatches: List<MaterialSwatch>)

object MaterialPalette {

  private val systemSwatch = MaterialSwatch(ClickerSettings.SYSTEM_PALETTE, "", R.color.auto_primary, R.style.ThemeOverlay_AutoClicker_Palette_System)
  val families = GENERATED_MATERIAL_FAMILIES
  val swatches = families.flatMap { it.swatches }
  private val swatchesById = (swatches + systemSwatch).associateBy { it.id }

  fun find(id: String): MaterialSwatch = swatchesById[id] ?: systemSwatch

  fun name(context: Context, id: String): String {
    return when (id) {
      ClickerSettings.SYSTEM_PALETTE  -> context.getString(R.string.palette_system)
      else                           -> {
        val swatch = find(id)
        val family = families.firstOrNull { it.swatches.contains(swatch) }
          ?: return context.getString(R.string.palette_system)
        val name = context.getString(family.nameResource)
        if ("Black" == swatch.tone || "White" == swatch.tone) {
          name
        } else {
          "$name ${swatch.tone}"
        }
      }
    }
  }

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
