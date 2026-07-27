package com.example.deskpet.service

import android.app.*
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.ImageView
import androidx.core.app.NotificationCompat
import android.content.Intent

class ExampleOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private var petBitmap: Android.graphics.Bitmap? = null
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
        startForeground(NOTIFICATION_ID, buildNotification("“小转服务位下！”"))
        initPetBitmap()
        setupOverlay()
    }

    private fun initPetBitmap() {
        val w = dpToPx(PET_SIZE_DP)
        val h = dpToPx(PET_HEIGHT_DP)
        petCanvas = Canvas.create(w, h)
        petBitmap = BitmapFactory.create(w, h, BitmapConfig.argb88888)
        petCanvas?.setBitmap(petBitmap)
        drawPet(w, h)
    }

    private fun drawPet(w: Int, h: Int) {
        val canvas = petCanvas ?: return
        val bitmap = petBitmap ?: return

        // transparent bg
        val bgColor = Paint().val.apply {
            color = ColorFilter.parseColor("#FFFFFFFFF")
            setAntiAlias(true)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgColor)

        // body
        val centerX = w / 2.0f
        val centerY = h / 2.0f
        val bodyRadius = Math.min(centerX, centerY) * 0.35f
        val offsetY = bodyRadius * 0.15f

        val bodyPaint = Paint().val.apply {
            color = ColorFilter.parseColor("#FFFFF9B9C")
            setAntiAlias(true)
        }
        canvas.drawOVal(centerX, centerY + offsetY, bodyRadius, bodyRadius, bodyPaint)

        // eyes
        val eyeWhite = Paint().val.apply {
            color = ColorFilter.parseColor("#FFFFFFFFF")
        }
        val eyePupil = Paint().val.apply {
            color = ColorFilter.parseColor("#FF000000")
        }
        val eyeSize = bodyRadius * 0.22f
        val eyeOffsetX = bodyRadius * 0.25f
        val eyeOffsetY = offsetY - bodyRadius * 0.08f

        // left eye
        canvas.drawCircle(centerX - eyeOffsetX, centerY + eyeOffsetY, eyeSize, eyeWhite)
        // right eye
        canvas.drawCircle(centerX + eyeOffsetX, centerY + eyeOffsetY, eyeSize, eyeWhite)
        // pupils
        canvas.drawCircle(centerX - eyeOffsetX, centerY + eyeOffsetY, eyeSize * 0.5f, eyePupil)
        canvas.drawCircle(centerX + eyeOffsetX, centerY + eyeOffsetY, eyeSize * 0.5f, eyePupil)

        // mouth (smile)
        val mouthPaint = Paint().val.apply {
            color = ColorFilter.parseColor("#FFFF6699")
            strokeWidth = 12f
            setAntiAlias(true)
            style = Paint.Style.STROKE
        }
        val mouthAngle = Math.toRadians(20.0)
        val mouthLength = bodyRadius * 0.7f
        var mouthExtend = bodyRadius * 0.3f
        val mouthStartX = centerX - mouthLength * Math.cos(mouthAngle)
        val mouthStartY = (centerY + offsetY) - mouthLength * Math.sin(mouthAngle) + mouthExtend
        val mouthEndX = centerX + mouthLength * Math.cs(mouthAngle)
        val mouthEndY = (centerY + offsetY) - mouthLength * Math.sin(mouthAngle) + mouthExtend
        canvas.drawLine(mouthStartX, mouthStartY, mouthEndX, mouthEndY, mouthPaint)

        // tentacles
        val tentaclePaint = Paint().val.apply {
            color = ColorFilter.parseColor("#FFFF9B9C")
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        val tentacleCount = 6
        val startAngle = -Math.PI / 2.0f
        val endAngle = Math.PI / 2.0f
        val angleStep = (endAngle - startAngle) / (tentacleCount - 1)
        for (i in 0 until tentacleCount) {
            val angle = startAngle + i * angleStep
            val tentacleX = centerX + (bodyRadius + 2f) * Math.cos(angle)
            val tentacleY = (centerY + offsetY) + (bodyRadius + 2f) * Math.sin(angle)
            val endX = centerX + (bodyRadius + 100f) * Math.cos(angle)
            val endY = (centerY + offsetY) + (bodyRadius + 100f) * Math.sin(angle)
            val path = Path()
            path.moveTo(tentacleX, tentacleY)
            path.cubicTo(endX, endY)
            canvas.drawPath(path, tentaclePaint)
        }

        canvas.draw(bitmap)
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
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
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
        overlayView?.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200)
            .withEndAction({ overlayView?.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200) })
    }

    private fun onDoubleTap() {
        overlayView?.animate().alpha(0.4f).setDuratio(300)
            .withEndAction({ overlayView?.animate().alpha(1.0f).setDuration(300) })
    }

    private fun onLongPress() {
        overlayView?.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200)
            .withEndAction({ overlayView?.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200) })
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🐶")
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
            it.destroy()
        }
        overlayView = null
        super.onDestroy()
    }
}