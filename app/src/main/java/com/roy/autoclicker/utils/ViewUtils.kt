package com.roy.autoclicker.utils

import android.os.SystemClock
import android.view.View

object ViewUtils {

  fun View.setClickAction(callback: View.OnClickListener) {
    this.setClickAction(duration = 200L, callback)
  }

  fun View.setClickAction(duration: Long, callback: View.OnClickListener) {
    var lastClickTime = 0L
    var clicked = false
    setOnClickListener { view ->
      val now = SystemClock.elapsedRealtime()
      if (!clicked || 0L >= duration || duration <= now - lastClickTime) {
        clicked = true
        lastClickTime = now
        callback.onClick(view)
      }
    }
  }
}