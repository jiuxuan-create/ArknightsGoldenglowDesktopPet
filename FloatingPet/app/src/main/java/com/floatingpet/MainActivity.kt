package com.floatingpet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.floatingpet.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 悬浮窗权限请求
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateOverlayButton()
        }

    // 相册选图
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // 持久化读取权限
                contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Settings.setCustomImage(this, it.toString())
                Toast.makeText(this, "已设置自定义桌宠图片", Toast.LENGTH_SHORT).show()
                // 如果桌宠正在显示，刷新它
                if (Settings.isShown(this)) {
                    startService(Intent(this, FloatingPetService::class.java).apply {
                        action = FloatingPetService.ACTION_REFRESH
                    })
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ---- 大小滑杆 ----
        binding.sizeSeek.max = 300
        binding.sizeSeek.progress = Settings.getSizeDp(this)
        binding.sizeValue.text = "${Settings.getSizeDp(this)} dp"
        binding.sizeSeek.setOnSeekBarChangeListener(object : SimpleSeek() {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                binding.sizeValue.text = "$p dp"
                Settings.setSizeDp(this@MainActivity, p)
                pushUpdate()
            }
        })

        // ---- 亮度滑杆：0.2 ~ 1.5 ----
        binding.brightnessSeek.max = 130
        binding.brightnessSeek.progress =
            ((Settings.getBrightness(this) - 0.2f) / 1.3f * 100).toInt()
        binding.brightnessValue.text = "%.2f".format(Settings.getBrightness(this))
        binding.brightnessSeek.setOnSeekBarChangeListener(object : SimpleSeek() {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val v = 0.2f + p / 100f
                binding.brightnessValue.text = "%.2f".format(v)
                Settings.setBrightness(this@MainActivity, v)
                pushUpdate()
            }
        })

        // ---- 悬浮窗权限按钮 ----
        binding.btnPermission.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "已有悬浮窗权限", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }

        // ---- 显示/隐藏桌宠 ----
        binding.btnToggle.text =
            if (Settings.isShown(this)) "隐藏桌宠" else "显示桌宠"
        binding.btnToggle.setOnClickListener {
            if (Settings.isShown(this)) {
                stopService(Intent(this, FloatingPetService::class.java))
                Settings.setShown(this, false)
                binding.btnToggle.text = "显示桌宠"
            } else {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val svc = Intent(this, FloatingPetService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(svc)
                } else {
                    startService(svc)
                }
                Settings.setShown(this, true)
                binding.btnToggle.text = "隐藏桌宠"
            }
        }

        // ---- 选择自定义图片 ----
        binding.btnPickImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // ---- 恢复默认图片 ----
        binding.btnResetImage.setOnClickListener {
            Settings.setCustomImage(this, null)
            Toast.makeText(this, "已恢复默认桌宠", Toast.LENGTH_SHORT).show()
            pushUpdate()
        }
    }

    override fun onResume() {
        super.onResume()
        updateOverlayButton()
    }

    private fun updateOverlayButton() {
        binding.btnPermission.text =
            if (Settings.canDrawOverlays(this)) "✓ 悬浮窗权限已开启"
            else "点击开启悬浮窗权限"
    }

    /** 桌宠正在显示时，把新的大小/亮度推送给悬浮窗服务 */
    private fun pushUpdate() {
        if (Settings.isShown(this)) {
            startService(Intent(this, FloatingPetService::class.java).apply {
                action = FloatingPetService.ACTION_REFRESH
            })
        }
    }

    /** 简化 SeekBar 回调 */
    abstract class SimpleSeek : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }
}
