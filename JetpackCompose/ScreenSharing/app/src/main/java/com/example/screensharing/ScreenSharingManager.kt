package com.example.screensharing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.opentok.android.BaseVideoCapturer

class ScreenSharingCapturer(
    private val context: Context,
    private val mediaProjection: MediaProjection
) : BaseVideoCapturer() {

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    private var capturing = false
    private val fps = 15
    private var width = 0
    private var height = 0

    init {
        initDisplayMetrics()
    }

    private fun initDisplayMetrics() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        windowManager?.let {
            val displayMetrics = DisplayMetrics()
            it.defaultDisplay.getMetrics(displayMetrics)
            width = displayMetrics.widthPixels
            height = displayMetrics.heightPixels
        }
    }

    @SuppressLint("WrongConstant")
    override fun init() {
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        startBackgroundThread()
    }

    private fun createVirtualDisplay() {
        mediaProjection.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    virtualDisplay?.let {
                        it.release()
                        virtualDisplay = null
                    }
                }
            },
            Handler(Looper.getMainLooper())
        )

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenSharing",
            width,
            height,
            context.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            backgroundHandler
        )

        imageReader?.setOnImageAvailableListener(
            { reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    provideBufferFrame(planes[0].buffer, ABGR, width, height, 0, false)
                    image.close()
                }
            },
            backgroundHandler
        )
    }

    override fun startCapture(): Int {
        capturing = true
        return 0
    }

    override fun stopCapture(): Int {
        capturing = false
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection.stop()
        stopBackgroundThread()
        return 0
    }

    override fun isCaptureStarted(): Boolean = capturing

    override fun getCaptureSettings(): CaptureSettings {
        return CaptureSettings().apply {
            this.fps = this@ScreenSharingCapturer.fps
            this.width = this@ScreenSharingCapturer.width
            this.height = this@ScreenSharingCapturer.height
            format = ABGR
        }
    }

    override fun destroy() {}

    override fun onPause() {}

    override fun onResume() {}

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("ScreenCapture").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
        createVirtualDisplay()
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}
