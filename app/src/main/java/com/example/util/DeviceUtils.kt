package com.example.util

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.runtime.compositionLocalOf

val LocalIsTelevision = compositionLocalOf { false }

object DeviceUtils {

    /**
     * Determines whether the current running environment is an Android TV / Television device
     * using Android's UiModeManager and Configuration.
     */
    fun isTelevision(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isUiModeTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val isConfigTv = (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = context.packageManager.hasSystemFeature("android.software.leanback")
        return isUiModeTv || isConfigTv || hasLeanback
    }

    /**
     * Configures activity screen orientation based on device type:
     * - Television: Lock to landscape (SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
     * - Phone & Tablet: Adaptive full rotation (SCREEN_ORIENTATION_FULL_SENSOR)
     */
    fun configureDeviceOrientation(activity: Activity) {
        if (isTelevision(activity)) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
    }
}
