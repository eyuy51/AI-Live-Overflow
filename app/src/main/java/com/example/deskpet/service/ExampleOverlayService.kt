package com.example.deskpet.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.*

class ExampleOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private var petBitmap: Bitmap? = null

    companion object {
        private const val CHANNEL_ID = "pet_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PET_SIZE_DP = 140
        private const val PET_HEIGHT_DP = 150
        private const val TAG = "DeskPet"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("小章鱼来啦！"))
        // 延迟一下确保服务完全就绪
        Handler(Looper.getMainLooper()).postDelayed({
            initPetBitmap()
            setupOverlay()
        }, 300)
    }

    private fun initPetBitmap() {
        val w = dpToPx(PET_SIZE_DP)
        val h = dpToPx(PET_HEIGHT_DP)
        Log.d(TAG, "Creating bitmap: ${w}x${h}")
        petBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(petBitmap!!)
        drawPet(canvas, w, h)
    }

    private fun drawPet(canvas: Canvas, w: Int, h: Int) {
        val centerX = w / 2.0f
        val centerY = h / 2.0f + 10f
        val bodyRadius = min(centerX, centerY) * 0.35f

        // 背景透明
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // 触手（画在身体后面）
        val tentaclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 155, 156)
            strokeWidth = 6f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val tentacleCount = 6
        val startAngle = -PI / 2.0 - 0.3
        val endAngle = PI / 2.0 + 0.3
        val angleStep = (endAngle - startAngle) / (tentacleCount - 1)
        for (i in 0 until tentacleCount) {
            val angle = startAngle + i * angleStep
            val startX = centerX + (bodyRadius + 2f) * cos(angle).toFloat()
            val startY = centerY + (bodyRadius + 2f) * sin(angle).toFloat()
            val midX = centerX + (bodyRadius + 55f) * cos(angle).toFloat()
            val midY = centerY + (bodyRadius + 55f) * sin(angle).toFloat()
            val endX = centerX + (bodyRadius + 90f) * cos(angle).toFloat()
            val endY = centerY + (bodyRadius + 90f) * sin(angle).toFloat()
            val path = Path()
            path.moveTo(startX, startY)
            path.quadTo(midX, midY, endX, endY)
            canvas.drawPath(path, tentaclePaint)
        }

        // 身体 - 粉色椭圆形
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 155, 156)
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            centerX - bodyRadius, centerY - bodyRadius * 0.85f,
            centerX + bodyRadius, centerY + bodyRadius * 0.85f,
            bodyPaint
        )

        // 身体轮廓线
        val bodyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(240, 130, 132)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawOval(
            centerX - bodyRadius, centerY - bodyRadius * 0.85f,
            centerX + bodyRadius, centerY + bodyRadius * 0.85f,
            bodyStrokePaint
        )

        // 眼睛 - 黑色大眼珠
        val eyeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val eyePupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30)
            style = Paint.Style.FILL
        }
        val eyeRadius = bodyRadius * 0.18f
        val eyeOffsetX = bodyRadius * 0.28f
        val eyeY = centerY - bodyRadius * 0.15f

        // 左眼白
        canvas.drawCircle(centerX - eyeOffsetX, eyeY, eyeRadius * 1.3f, eyeWhitePaint)
        // 左瞳孔
        canvas.drawCircle(centerX - eyeOffsetX, eyeY, eyeRadius, eyePupilPaint)
        // 左高光
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(centerX - eyeOffsetX + eyeRadius * 0.4f, eyeY - eyeRadius * 0.4f, eyeRadius * 0.35f, highlightPaint)

        // 右眼白
        canvas.drawCircle(centerX + eyeOffsetX, eyeY, eyeRadius * 1.3f, eyeWhitePaint)
        // 右瞳孔
        canvas.drawCircle(centerX + eyeOffsetX, eyeY, eyeRadius, eyePupilPaint)
        // 右高光
        canvas.drawCircle(centerX + eyeOffsetX + eyeRadius * 0.4f, eyeY - eyeRadius * 0.4f, eyeRadius * 0.35f, highlightPaint)

        // 腮红
        val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 255, 150, 150)
            style = Paint.Style.FILL
        }
        val blushRadius = bodyRadius * 0.2f
        canvas.drawCircle(centerX - bodyRadius * 0.5f, eyeY + bodyRadius * 0.3f, blushRadius, blushPaint)
        canvas.drawCircle(centerX + bodyRadius * 0.5f, eyeY + bodyRadius * 0.3f, blushRadius, blushPaint)

        // 嘴巴 - 微笑弧线
        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 80, 120)
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val mouthPath = Path()
        val mouthCenterY = eyeY + bodyRadius * 0.6f
        mouthPath.moveTo(centerX - bodyRadius * 0.35f, mouthCenterY)
        mouthPath.quadTo(centerX, mouthCenterY + bodyRadius * 0.3f, centerX + bodyRadius * 0.35f, mouthCenterY)
        canvas.drawPath(mouthPath, mouthPaint)

        // 小舌头
        val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 100, 130)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, mouthCenterY + bodyRadius * 0.18f, bodyRadius * 0.08f, tonguePaint)

        Log.d(TAG, "Pet drawn successfully")
    }

    private fun setupOverlay() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            params = WindowManager.LayoutParams(
                dpToPx(PET_SIZE_DP),
                dpToPx(PET_HEIGHT_DP),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 200
            }

            overlayView = object : ImageView(this) {
                override fun onDraw(canvas: Canvas?) {
                    super.onDraw(canvas)
                    // 重新绘制确保内容显示
                }
            }.apply {
                setImageBitmap(petBitmap)
                setBackgroundColor(Color.TRANSPARENT)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setOnTouchListener(createTouchListener())
            }

            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay: ${e.message}", e)
        }
    }

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastTapTime = 0L
    private var touchStartTime = 0L
    private var hasMoved = false

    private fun createTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    hasMoved = false
                    Log.d(TAG, "Touch DOWN at ($initialX, $initialY)")
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        hasMoved = true
                        params?.let {
                            it.x = initialX + dx
                            it.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(overlayView, it)
                            } catch (e: Exception) {
                                Log.e(TAG, "updateViewLayout failed: ${e.message}")
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = System.currentTimeMillis() - touchStartTime
                    Log.d(TAG, "Touch UP - moved=$hasMoved, elapsed=$elapsed")
                    if (!hasMoved) {
                        when {
                            elapsed > 600 -> onLongPress()
                            System.currentTimeMillis() - lastTapTime < 300 -> onDoubleTap()
                            else -> {
                                lastTapTime = System.currentTimeMillis()
                                onTap()
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun onTap() {
        Log.d(TAG, "onTap")
        overlayView?.let { v ->
            v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150)
                .withEndAction { v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150) }
        }
    }

    private fun onDoubleTap() {
        Log.d(TAG, "onDoubleTap")
        overlayView?.let { v ->
            v.animate().alpha(0.3f).setDuration(200)
                .withEndAction { v.animate().alpha(1.0f).setDuration(200) }
        }
    }

    private fun onLongPress() {
        Log.d(TAG, "onLongPress")
        overlayView?.let { v ->
            v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150)
                .withEndAction { v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150) }
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🐙 小章鱼")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "宠物桌宠",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        try {
            overlayView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "removeView failed: ${e.message}")
        }
        overlayView = null
        petBitmap?.recycle()
        petBitmap = null
        super.onDestroy()
    }
}
