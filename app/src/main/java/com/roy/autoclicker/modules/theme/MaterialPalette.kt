package com.roy.autoclicker.modules.theme

import android.content.Context
import android.graphics.Color
import com.roy.autoclicker.R
import com.roy.autoclicker.common.settings.ClickerSettings

data class MaterialSwatch(val id: String, val tone: String, val colorResource: Int, val styleResource: Int)

data class MaterialFamily(val id: String, val nameResource: Int, val swatches: List<MaterialSwatch>)

object MaterialPalette {

  val families = GENERATED_MATERIAL_FAMILIES
  val swatches = families.flatMap { it.swatches }
  private val swatchesById = swatches.associateBy { it.id }
  private val defaultSwatch = swatchesById.getValue(ClickerSettings.DEFAULT_PALETTE)

  fun find(id: String): MaterialSwatch = swatchesById[id] ?: defaultSwatch

  fun name(context: Context, id: String): String {
    val swatch = find(id)
    val family = families.first { it.swatches.contains(swatch) }
    val name = context.getString(family.nameResource)
    return if ("Black" == swatch.tone || "White" == swatch.tone) {
      name
    } else {
      "$name ${swatch.tone}"
    }
  }

  fun applyTheme(context: Context, id: String) {
    context.theme.applyStyle(find(id).styleResource, true)
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
