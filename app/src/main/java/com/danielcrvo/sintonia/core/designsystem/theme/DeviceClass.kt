package com.danielcrvo.sintonia.core.designsystem.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

enum class DeviceClass {
    Phone,
    Tv,
    ;

    companion object {
        fun detect(context: Context): DeviceClass {
            val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            return if (uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) Tv else Phone
        }
    }
}
