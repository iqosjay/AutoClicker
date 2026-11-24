package com.roy.autoclicker.common.utils

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.roy.autoclicker.R
import com.roy.autoclicker.common.utils.ViewUtils.setClickAction

object EmailUtils {

  fun TextView.setEmailAction(recipient: String) {
    paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
    setClickAction {
      val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", recipient, null))
      try {
        context.startActivity(intent)
      } catch (x: Throwable) {
        Log.w("EmailUtils", "Unable to open email app", x)
        Toast.makeText(context, R.string.email_app_unavailable, Toast.LENGTH_LONG).show()
      }
    }
  }
}
