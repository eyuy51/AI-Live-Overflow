package com.example.deskpet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.example.deskpet.service.ExampleOverlayService

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DeskPet", "MainActivity onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.d("DeskPet", "悬浮窗权限未授予，跳转设置")
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            finish()
        } else {
            startOverlayService()
            // 延迟finish确保服务有足够时间启动
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d("DeskPet", "MainActivity finish")
                finish()
            }, 500)
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, ExampleOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
            Log.d("DeskPet", "startForegroundService called")
        } else {
            startService(intent)
            Log.d("DeskPet", "startService called")
        }
        Toast.makeText(this, "悬浮窗已启动 🐙", Toast.LENGTH_SHORT).show()
    }
}
