package com.floatingpet

import android.content.Context

/**
 * 全局设置：用 SharedPreferences 持久化桌宠大小、亮度、自定义图片路径、位置等
 */
object Settings {
    private const val PREFS = "floating_pet_prefs"

    // 桌宠基础尺寸（dp）
    const val KEY_SIZE_DP = "size_dp"
    const val KEY_BRIGHTNESS = "brightness"
    const val KEY_CUSTOM_IMAGE = "custom_image_path"
    const val KEY_PET_X = "pet_x"
    const val KEY_PET_Y = "pet_y"
    const val KEY_IS_SHOWN = "is_shown"
    const val KEY_IGNORE_WINDOWS = "ignore_windows"

    // 默认值
    const val DEFAULT_SIZE_DP = 120      // 默认宽度 120dp
    const val DEFAULT_BRIGHTNESS = 1.0f  // 亮度 1.0 = 原亮度

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSizeDp(ctx: Context): Int =
        prefs(ctx).getInt(KEY_SIZE_DP, DEFAULT_SIZE_DP)

    fun setSizeDp(ctx: Context, dp: Int) =
        prefs(ctx).edit().putInt(KEY_SIZE_DP, dp).apply()

    fun getBrightness(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS)

    fun setBrightness(ctx: Context, v: Float) =
        prefs(ctx).edit().putFloat(KEY_BRIGHTNESS, v).apply()

    fun getCustomImage(ctx: Context): String? =
        prefs(ctx).getString(KEY_CUSTOM_IMAGE, null)

    fun setCustomImage(ctx: Context, path: String?) =
        prefs(ctx).edit().putString(KEY_CUSTOM_IMAGE, path).apply()

    fun getPetX(ctx: Context): Int = prefs(ctx).getInt(KEY_PET_X, 0)
    fun setPetX(ctx: Context, x: Int) = prefs(ctx).edit().putInt(KEY_PET_X, x).apply()

    fun getPetY(ctx: Context): Int = prefs(ctx).getInt(KEY_PET_Y, 0)
    fun setPetY(ctx: Context, y: Int) = prefs(ctx).edit().putInt(KEY_PET_Y, y).apply()

    fun isShown(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_IS_SHOWN, false)
    fun setShown(ctx: Context, shown: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_IS_SHOWN, shown).apply()
}
