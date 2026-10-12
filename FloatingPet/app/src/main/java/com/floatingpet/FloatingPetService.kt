package com.floatingpet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 悬浮窗桌宠服务：
 * - 在屏幕上显示一个可拖动的图片
 * - 支持调整大小、亮度
 * - 单击触发简单动画（缩放弹跳）
 * - 长按可进入"调整大小"模式
 */
class FloatingPetService : Service() {

    companion object {
        const val ACTION_REFRESH = "com.floatingpet.REFRESH"
        private const val CHANNEL_ID = "floating_pet_channel"
        private const val NOTIF_ID = 1001
    }

    private lateinit var wm: WindowManager
    private var petView: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // 拖动状态
    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f
    private var moved = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        addPetView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REFRESH) {
            updatePetView()
        }
        return START_STICKY
    }

    /** 前台服务通知 */
    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "桌宠悬浮窗",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "保持桌宠在屏幕上显示" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val n: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("桌宠运行中")
            .setContentText("拖动它到任意位置，单击互动")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, n)
    }

    /** 添加悬浮窗 */
    private fun addPetView() {
        if (petView != null) return

        val img = ImageView(this)
        img.scaleType = ImageView.ScaleType.FIT_CENTER
        setPetImage(img)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            // Android 8.0+ 必须用 TYPE_APPLICATION_OVERLAY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            // 不拦截后面的触摸事件；可聚焦
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = Settings.getPetX(this@FloatingPetService)
            y = Settings.getPetY(this@FloatingPetService)
        }

        applySizeAndBrightness(img, lp)
        wm.addView(img, lp)
        petView = img
        layoutParams = lp

        // 触摸监听：拖动 + 单击互动
        img.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    touchX = e.rawX
                    touchY = e.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).roundToInt()
                    val dy = (e.rawY - touchY).roundToInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    lp.x = initialX + dx
                    lp.y = initialY + dy
                    wm.updateViewLayout(img, lp)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        // 单击：弹跳动画
                        bounce(img)
                    }
                    // 保存位置
                    Settings.setPetX(this, lp.x)
                    Settings.setPetY(this, lp.y)
                    true
                }
                else -> false
            }
        }
    }

    /** 根据设置更新大小和亮度 */
    private fun applySizeAndBrightness(img: ImageView, lp: WindowManager.LayoutParams) {
        val sizePx = dpToPx(Settings.getSizeDp(this))
        lp.width = sizePx
        lp.height = sizePx
        val brightness = Settings.getBrightness(this)
        // ImageView.setColorFilter 用亮度矩阵近似
        val matrix = android.graphics.ColorMatrix().apply {
            setScale(brightness, brightness, brightness, 1f)
        }
        img.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
    }

    /** 刷新悬浮窗（大小/亮度/图片改变时调用） */
    private fun updatePetView() {
        petView?.let { img ->
            layoutParams?.let { lp ->
                setPetImage(img)
                applySizeAndBrightness(img, lp)
                wm.updateViewLayout(img, lp)
            }
        }
    }

    /** 设置桌宠图片：优先用用户导入的，否则用内置默认图 */
    private fun setPetImage(img: ImageView) {
        val customPath = Settings.getCustomImage(this)
        if (customPath != null) {
            try {
                val uri = android.net.Uri.parse(customPath)
                val stream = contentResolver.openInputStream(uri)
                val d = Drawable.createFromStream(stream, null)
                img.setImageDrawable(d)
                return
            } catch (_: Exception) {
                // 读取失败则回退默认
            }
        }
        // 默认内置图：用一个简单的圆形 drawable 占位
        img.setImageResource(R.drawable.default_pet)
    }

    /** 单击弹跳动画 */
    private fun bounce(img: ImageView) {
        img.animate().scaleX(1.15f).scaleY(0.85f).setDuration(80).withEndAction {
            img.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        petView?.let { wm.removeView(it) }
        petView = null
        Settings.setShown(this, false)
        super.onDestroy()
    }
}
