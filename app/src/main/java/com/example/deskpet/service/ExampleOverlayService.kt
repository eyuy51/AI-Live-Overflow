package com.example.deskpet.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
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
    private var petCanvas: Canvas? = null

    companion object {
        private const val CHANNEL_ID = "pet_overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PET_SIZE_DP = 160
        private const val PET_HEIGHT_DP = 170
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("小章鱼来啦！"))
        initPetBitmap()
        setupOverlay()
    }

    private fun initPetBitmap() {
        val w = dpToPx(PET_SIZE_DP)
        val h = dpToPx(PET_HEIGHT_DP)
        petBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        petCanvas = Canvas(petBitmap!!)
        drawPet(w, h)
    }

    private fun drawPet(w: Int, h: Int) {
        val canvas = petCanvas ?: return

        // transparent bg
        val bgColor = Paint().apply {
            color = Color.parseColor("#00FFFFFF")
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgColor)

        // body
        val centerX = w / 2.0f
        val centerY = h / 2.0f
        val bodyRadius = min(centerX, centerY) * 0.35f
        val offsetY = bodyRadius * 0.15f

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#FFFF9B9C")
            isAntiAlias = true
        }
        canvas.drawOval(
            centerX - bodyRadius, centerY + offsetY - bodyRadius,
            centerX + bodyRadius, centerY + offsetY + bodyRadius,
            bodyPaint
        )

        // eyes (豆豆眼 style)
        val eyePaint = Paint().apply {
            color = Color.parseColor("#FF000000")
        }
        val eyeSize = bodyRadius * 0.15f
        val eyeOffsetX = bodyRadius * 0.25f
        val eyeOffsetY = offsetY - bodyRadius * 0.1f

        // left eye - 豆豆眼
        canvas.drawCircle(centerX - eyeOffsetX, centerY + eyeOffsetY, eyeSize, eyePaint)
        // right eye - 豆豆眼
        canvas.drawCircle(centerX + eyeOffsetX, centerY + eyeOffsetY, eyeSize, eyePaint)

        // mouth (smile)
        val mouthPaint = Paint().apply {
            color = Color.parseColor("#FFFF6699")
            strokeWidth = 12f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        val mouthAngle = Math.toRadians(20.0)
        val mouthLength = bodyRadius * 0.7f
        val mouthExtend = bodyRadius * 0.3f
        val mouthStartX = centerX - mouthLength * cos(mouthAngle).toFloat()
        val mouthStartY = (centerY + offsetY) - mouthLength * sin(mouthAngle).toFloat() + mouthExtend
        val mouthEndX = centerX + mouthLength * cos(mouthAngle).toFloat()
        val mouthEndY = (centerY + offsetY) - mouthLength * sin(mouthAngle).toFloat() + mouthExtend
        canvas.drawLine(mouthStartX, mouthStartY, mouthEndX, mouthEndY, mouthPaint)

        // tentacles
        val tentaclePaint = Paint().apply {
            color = Color.parseColor("#FFFF9B9C")
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val tentacleCount = 6
        val startAngle = -PI / 2.0
        val endAngle = PI / 2.0
        val angleStep = (endAngle - startAngle) / (tentacleCount - 1)
        for (i in 0 until tentacleCount) {
            val angle = startAngle + i * angleStep
            val tentacleX = centerX + (bodyRadius + 2f) * cos(angle).toFloat()
            val tentacleY = (centerY + offsetY) + (bodyRadius + 2f) * sin(angle).toFloat()
            val cpX = centerX + (bodyRadius + 50f) * cos(angle).toFloat()
            val cpY = (centerY + offsetY) + (bodyRadius + 50f) * sin(angle).toFloat()
            val endX = centerX + (bodyRadius + 100f) * cos(angle).toFloat()
            val endY = (centerY + offsetY) + (bodyRadius + 100f) * sin(angle).toFloat()
            val path = Path()
            path.moveTo(tentacleX, tentacleY)
            path.cubicTo(cpX, cpY, cpX, cpY, endX, endY)
            canvas.drawPath(path, tentaclePaint)
        }
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            dpToPx(PET_SIZE_DP),
            dpToPx(PET_HEIGHT_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        overlayView = ImageView(this).apply {
            setImageBitmap(petBitmap)
            setOnTouchListener(createTouchListener())
        }

        windowManager?.addView(overlayView, params)
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
                            windowManager?.updateViewLayout(overlayView, it)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = System.currentTimeMillis() - touchStartTime
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
        overlayView?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(200)
            ?.withEndAction { overlayView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(200) }
    }

    private fun onDoubleTap() {
        overlayView?.animate()?.alpha(0.4f)?.setDuration(300)
            ?.withEndAction { overlayView?.animate()?.alpha(1.0f)?.setDuration(300) }
    }

    private fun onLongPress() {
        overlayView?.animate()?.scaleX(1.2f)?.scaleY(1.2f)?.setDuration(200)
            ?.withEndAction { overlayView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(200) }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🐙")
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
                "Pet",
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
        overlayView?.let {
            windowManager?.removeView(it)
        }
        overlayView = null
        petBitmap?.recycle()
        petBitmap = null
        super.onDestroy()
    }
}
