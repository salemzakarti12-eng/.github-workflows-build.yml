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
     * يكتشف ما إذا كان التطبيق يعمل حالياً داخل وضع Samsung DeX أو وضع سطح المكتب
     */
    fun isSamsungDexMode(context: Context): Boolean {
        return try {
            val config = context.resources.configuration
            val configClass = config.javaClass
            val dexEnabledField = configClass.getField("semDesktopModeEnabled")
            val dexEnabledValue = configClass.getField("SEM_DESKTOP_MODE_ENABLED")
            dexEnabledField.getInt(config) == dexEnabledValue.getInt(config)
        } catch (_: Exception) {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            val isDeskUiMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_DESK
            val isConfigDesk = (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_DESK
            isDeskUiMode || isConfigDesk
        }
    }

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
     * - Samsung DeX / Desktop: Allow unrestricted window resizing (SCREEN_ORIENTATION_UNSPECIFIED)
     * - Television: Lock to landscape (SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
     * - Phone & Tablet: Adaptive full rotation (SCREEN_ORIENTATION_FULL_SENSOR)
     */
    fun configureDeviceOrientation(activity: Activity) {
        when {
            isSamsungDexMode(activity) -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            isTelevision(activity) -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            else -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }
        }
    }
}
