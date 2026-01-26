package com.example.basicvideocapturer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.Display
import android.view.Surface
import android.hardware.display.DisplayManager
import com.opentok.android.BaseVideoCapturer
import com.opentok.android.Publisher
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.math.abs

class MirrorVideoCapturer(
    ctx: Context,
    resolution: Publisher.CameraCaptureResolution,
    fps: Publisher.CameraCaptureFrameRate
) : BaseVideoCapturer(), BaseVideoCapturer.CaptureSwitch {

    private val cameraManager: CameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var camera: CameraDevice? = null
    private var cameraThread: HandlerThread? = null
    private var cameraThreadHandler: Handler? = null
    private var cameraFrame: ImageReader? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraInfoCache: CameraInfoCache? = null
    private var cameraState: CameraState = CameraState.CLOSED
    private val display: Display = (ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY)!!
    private var displayOrientationCache: DisplayOrientationCache? = null
    private var cameraIndex: Int = 0
    private val frameDimensions: Size
    private val desiredFps: Int
    private var camFps: Range<Int>? = null

    private var executeAfterClosed: Runnable? = null
    private var executeAfterCameraOpened: Runnable? = null
    private var executeAfterCameraSessionConfigured: Runnable? = null

    private var cycleCameraInProgress = false
    private val lock = Any()

    private enum class CameraState {
        CLOSED,
        CLOSING,
        SETUP,
        OPEN,
        CAPTURE,
        CREATESESSION,
        ERROR
    }

    companion object {
        private const val PREFERRED_FACING_CAMERA = CameraMetadata.LENS_FACING_FRONT
        private const val PIXEL_FORMAT = ImageFormat.YUV_420_888
        private val TAG = MirrorVideoCapturer::class.java.simpleName

        private val rotationTable = SparseIntArray().apply {
            append(Surface.ROTATION_0, 0)
            append(Surface.ROTATION_90, 90)
            append(Surface.ROTATION_180, 180)
            append(Surface.ROTATION_270, 270)
        }

        private val resolutionTable = SparseArray<Size>().apply {
            append(Publisher.CameraCaptureResolution.LOW.ordinal, Size(352, 288))
            append(Publisher.CameraCaptureResolution.MEDIUM.ordinal, Size(640, 480))
            append(Publisher.CameraCaptureResolution.HIGH.ordinal, Size(1280, 720))
            append(Publisher.CameraCaptureResolution.HIGH_1080P.ordinal, Size(1920, 1080))
        }

        private val frameRateTable = SparseIntArray().apply {
            append(Publisher.CameraCaptureFrameRate.FPS_1.ordinal, 1)
            append(Publisher.CameraCaptureFrameRate.FPS_7.ordinal, 7)
            append(Publisher.CameraCaptureFrameRate.FPS_15.ordinal, 15)
            append(Publisher.CameraCaptureFrameRate.FPS_30.ordinal, 30)
        }
    }

    init {
        frameDimensions = resolutionTable.get(resolution.ordinal)!!
        desiredFps = frameRateTable.get(fps.ordinal)
        try {
            var camId = selectCamera(PREFERRED_FACING_CAMERA)
            /* if default camera facing direction is not found, use first camera */
            if (camId == null && cameraManager.cameraIdList.isNotEmpty()) {
                camId = cameraManager.cameraIdList[0]
            }
            setCameraIndex(findCameraIndex(camId))
            if (getCameraIndex() == -1) {
                Log.d(TAG, "Exception!. Camera Index cannot be -1.")
            } else {
                initCameraFrame()
            }
        } catch (exception: Exception) {
            handleException(exception)
        }
    }

    /* Observers/Notification callback objects */
    private val cameraObserver = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice.StateCallback onOpened() enter")
            cameraState = CameraState.OPEN
            this@MirrorVideoCapturer.camera = camera
            executeAfterCameraOpened?.run()
            executeAfterCameraOpened = null
            Log.d(TAG, "CameraDevice.StateCallback onOpened() exit")
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice.StateCallback onDisconnected() enter")
            try {
                executeAfterClosed = null
                this@MirrorVideoCapturer.camera?.close()
            } catch (exception: Exception) {
                handleException(exception)
            }
            Log.d(TAG, "CameraDevice.StateCallback onDisconnected() exit")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, "CameraDevice.StateCallback onError() enter")
            try {
                // In some rare cases we may receive an error:
                // - before the camera is opened and assigned, so let's just
                // explicitly close the camera in the callback parameter
                // - on Android 15 when app is moved to background
                this@MirrorVideoCapturer.camera?.close()
            } catch (exception: Exception) {
                handleException(exception)
            }
            Log.d(TAG, "CameraDevice.StateCallback onError() exit")
        }

        override fun onClosed(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice.StateCallback onClosed() enter.")
            super.onClosed(camera)
            cameraState = CameraState.CLOSED
            this@MirrorVideoCapturer.camera = null

            executeAfterClosed?.run()
            executeAfterClosed = null
            Log.d(TAG, "CameraDevice.StateCallback onClosed() exit.")
        }
    }

    private val frameObserver = ImageReader.OnImageAvailableListener { reader ->
        try {
            val frame = reader.acquireNextImage()
            if (frame == null
                || (frame.planes.isNotEmpty() && frame.planes[0].buffer == null)
                || (frame.planes.size > 1 && frame.planes[1].buffer == null)
                || (frame.planes.size > 2 && frame.planes[2].buffer == null)
            ) {
                Log.d(TAG, "onImageAvailable frame provided has no image data")
                return@OnImageAvailableListener
            }
            if (CameraState.CAPTURE == cameraState) {
                provideBufferFramePlanar(
                    frame.planes[0].buffer,
                    frame.planes[1].buffer,
                    frame.planes[2].buffer,
                    frame.planes[0].pixelStride,
                    frame.planes[0].rowStride,
                    frame.planes[1].pixelStride,
                    frame.planes[1].rowStride,
                    frame.planes[2].pixelStride,
                    frame.planes[2].rowStride,
                    frame.width,
                    frame.height,
                    calculateCamRotation(),
                    isFrontCamera()
                )
            }
            frame.close()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "ImageReader.acquireNextImage() throws error !")
            throw Camera2Exception(e.message)
        }
    }

    private val captureSessionObserver = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(TAG, "CameraCaptureSession.StateCallback onConfigured() enter.")
            try {
                cameraState = CameraState.CAPTURE
                captureSession = session
                val captureRequest = captureRequestBuilder!!.build()
                captureSession!!.setRepeatingRequest(captureRequest, captureNotification, null)
            } catch (exception: Exception) {
                handleException(exception)
            }

            executeAfterCameraSessionConfigured?.run()
            executeAfterCameraSessionConfigured = null
            synchronized(lock) {
                if (cycleCameraInProgress) {
                    cycleCameraInProgress = false
                    onCameraChanged(getCameraIndex())
                }
            }
            Log.d(TAG, "CameraCaptureSession.StateCallback onConfigured() exit.")
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.d(TAG, "CameraCaptureSession.StateCallback onFailed() enter.")
            cameraState = CameraState.ERROR
            Log.d(TAG, "CameraCaptureSession.StateCallback onFailed() exit.")
        }

        override fun onClosed(session: CameraCaptureSession) {
            Log.d(TAG, "CameraCaptureSession.StateCallback onClosed() enter.")
            camera?.close()
            Log.d(TAG, "CameraCaptureSession.StateCallback onClosed() exit.")
        }
    }

    private val captureNotification = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
        }
    }

    /* caching of camera characteristics & display orientation for performance */
    private class CameraInfoCache(info: CameraCharacteristics) {
        private val frontFacing: Boolean
        private val sensorOrientation: Int

        init {
            /* its actually faster to cache these results then to always look
               them up, and since they are queried every frame...
             */
            frontFacing = (info.get(CameraCharacteristics.LENS_FACING)
                ?: CameraCharacteristics.LENS_FACING_BACK) == CameraCharacteristics.LENS_FACING_FRONT
            sensorOrientation = info.get(CameraCharacteristics.SENSOR_ORIENTATION)!!.toInt()
        }

        fun isFrontFacing(): Boolean = frontFacing

        fun sensorOrientation(): Int = sensorOrientation
    }

    private class DisplayOrientationCache(
        private val display: Display,
        private val handler: Handler
    ) : Runnable {
        private var displayRotation: Int

        companion object {
            private const val POLL_DELAY_MS = 750L /* 750 ms */
        }

        init {
            displayRotation = rotationTable.get(display.rotation)
            handler.postDelayed(this, POLL_DELAY_MS)
        }

        fun getOrientation(): Int = displayRotation

        override fun run() {
            displayRotation = rotationTable.get(display.rotation)
            handler.postDelayed(this, POLL_DELAY_MS)
        }
    }

    /* custom exceptions */
    class Camera2Exception(message: String?) : RuntimeException(message)

    private fun doInit() {
        Log.d(TAG, "doInit() enter")
        cameraInfoCache = null
        // start camera looper thread
        startCamThread()
        // start display orientation polling
        startDisplayOrientationCache()
        // open selected camera
        initCamera()
        Log.d(TAG, "doInit() exit")
    }

    /**
     * Initializes the video capturer.
     */
    @Synchronized
    override fun init() {
        Log.d(TAG, "init() enter")

        doInit()
        cameraState = CameraState.SETUP
        Log.d(TAG, "init() exit")
    }

    private fun doStartCapture() {
        Log.d(TAG, "doStartCapture() enter")
        cameraState = CameraState.CREATESESSION
        try {
            // create camera preview request
            if (isFrontCamera()) {
                captureRequestBuilder = camera!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequestBuilder!!.addTarget(cameraFrame!!.surface)
                captureRequestBuilder!!.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, camFps)
                captureRequestBuilder!!.set(
                    CaptureRequest.CONTROL_MODE,
                    CaptureRequest.CONTROL_MODE_USE_SCENE_MODE
                )
                captureRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                captureRequestBuilder!!.set(
                    CaptureRequest.CONTROL_SCENE_MODE,
                    CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY
                )
            } else {
                captureRequestBuilder = camera!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                captureRequestBuilder!!.addTarget(cameraFrame!!.surface)
                captureRequestBuilder!!.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, camFps)
                captureRequestBuilder!!.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }
            camera!!.createCaptureSession(
                Collections.singletonList(cameraFrame!!.surface),
                captureSessionObserver,
                null
            )
        } catch (exception: CameraAccessException) {
            handleException(exception)
        }
        Log.d(TAG, "doStartCapture() exit")
    }

    /**
     * Starts capturing video.
     */
    @Synchronized
    override fun startCapture(): Int {
        Log.d(TAG, "startCapture() enter (cameraState: $cameraState)")
        val resume = Runnable {
            initCamera()
            scheduleStartCapture()
        }
        when (cameraState) {
            CameraState.CLOSING -> executeAfterClosed = resume
            CameraState.CLOSED -> resume.run()
            else -> scheduleStartCapture()
        }
        Log.d(TAG, "startCapture() exit")
        return 0
    }

    /**
     * Starts capturing video.
     */
    @Synchronized
    private fun scheduleStartCapture() {
        Log.d(TAG, "scheduleStartCapture() enter (cameraState: $cameraState)")
        when {
            camera != null && CameraState.OPEN == cameraState -> doStartCapture()
            CameraState.SETUP == cameraState -> {
                Log.d(TAG, "camera not yet ready, queuing the start until camera is opened.")
                executeAfterCameraOpened = Runnable { doStartCapture() }
            }
            CameraState.CREATESESSION == cameraState -> {
                Log.d(TAG, "Camera session creation already requested")
            }
            else -> {
                Log.d(TAG, "Start Capture called before init successfully completed.")
            }
        }
        Log.d(TAG, "scheduleStartCapture() exit")
    }

    /**
     * Stops capturing video.
     */
    @Synchronized
    override fun stopCapture(): Int {
        Log.d(TAG, "stopCapture() enter (cameraState: $cameraState)")
        when {
            camera != null && captureSession != null && CameraState.CAPTURE == cameraState -> {
                try {
                    captureSession!!.stopRepeating()
                } catch (exception: CameraAccessException) {
                    handleException(exception)
                } catch (exception: IllegalStateException) {
                    // On Android 15, IllegalStateException exception is raised because camera is already closed
                    handleException(exception)
                }
                captureSession!!.close()
                cameraInfoCache = null
                cameraState = CameraState.CLOSING
            }
            camera != null && CameraState.OPEN == cameraState -> {
                cameraState = CameraState.CLOSING
                camera!!.close()
            }
            CameraState.SETUP == cameraState -> {
                executeAfterCameraOpened = Runnable {
                    cameraState = CameraState.CLOSING
                    camera?.close()
                }
            }
            CameraState.CREATESESSION == cameraState -> {
                executeAfterCameraSessionConfigured = Runnable {
                    captureSession!!.close()
                    cameraState = CameraState.CLOSING
                    executeAfterCameraSessionConfigured = null
                }
            }
        }
        Log.d(TAG, "stopCapture exit")
        return 0
    }

    /**
     * Destroys the BaseVideoCapturer object.
     */
    @Synchronized
    override fun destroy() {
        Log.d(TAG, "destroy() enter")

        /* stop display orientation polling */
        stopDisplayOrientationCache()

        /* stop camera message thread */
        stopCamThread()

        /* close ImageReader here */
        cameraFrame!!.close()
        Log.d(TAG, "destroy() exit")
    }

    /**
     * Whether video is being captured (true) or not (false).
     */
    override fun isCaptureStarted(): Boolean {
        return cameraState == CameraState.CAPTURE
    }

    /**
     * Returns the settings for the video capturer.
     */
    @Synchronized
    override fun getCaptureSettings(): CaptureSettings {
        val settings = CaptureSettings()
        settings.fps = desiredFps
        settings.width = cameraFrame?.width ?: -1
        settings.height = cameraFrame?.height ?: -1
        settings.format = NV21
        settings.expectedDelay = 0
        settings.mirrorInLocalRender = frameMirrorX

        return settings
    }

    /**
     * Call this method when the activity pauses. When you override this method, implement code
     * to respond to the activity being paused. For example, you may pause capturing audio or video.
     *
     * @see #onResume()
     */
    @Synchronized
    override fun onPause() {
        // PublisherKit.onPause() already calls setPublishVideo(false), which stops the camera
        // Nothing to do here
    }

    /**
     * Call this method when the activity resumes. When you override this method, implement code
     * to respond to the activity being resumed. For example, you may resume capturing audio
     * or video.
     *
     * @see #onPause()
     */
    override fun onResume() {
        // PublisherKit.onResume() already calls setPublishVideo(true), which resumes the camera
        // Nothing to do here
    }

    private fun isDepthOutputCamera(cameraId: String): Boolean {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        if (capabilities != null) {
            for (capability in capabilities) {
                if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) {
                    Log.d(TAG, " REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT => TRUE")
                    return true
                }
            }
        }
        Log.d(TAG, " REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT => FALSE")
        return false
    }

    private fun isBackwardCompatible(cameraId: String): Boolean {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        if (capabilities != null) {
            for (capability in capabilities) {
                if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) {
                    Log.d(TAG, "REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE => TRUE")
                    return true
                }
            }
        }
        Log.d(TAG, "REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE => FALSE")
        return false
    }

    private fun getCameraOutputSizes(cameraId: String): Array<Size> {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val dimMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        return dimMap?.getOutputSizes(PIXEL_FORMAT) ?: emptyArray()
    }

    private fun getNextSupportedCameraIndex(): Int {
        val cameraIds = cameraManager.cameraIdList
        val numCameraIds = cameraIds.size

        // Cycle through all the cameras to find the next one with supported
        // outputs
        for (i in 0 until numCameraIds) {
            // We use +1 so that the algorithm will rollover and check the
            // current camera too. At minimum, the current camera *should* have
            // supported outputs.
            val nextCameraIndex = (getCameraIndex() + i + 1) % numCameraIds
            val outputSizes = getCameraOutputSizes(cameraIds[nextCameraIndex])
            val hasSupportedOutputs = outputSizes.isNotEmpty()

            // Best guess is that the crash is happening when sdk is
            // trying to open depth sensor cameras while doing cycleCamera() function.
            val isDepthOutputCamera = isDepthOutputCamera(cameraIds[nextCameraIndex])
            val isBackwardCompatible = isBackwardCompatible(cameraIds[nextCameraIndex])

            if (hasSupportedOutputs && isBackwardCompatible && !isDepthOutputCamera) {
                return nextCameraIndex
            }
        }

        // No supported cameras found
        return -1
    }

    @Synchronized
    override fun cycleCamera() {
        synchronized(lock) {
            if (cycleCameraInProgress) {
                Log.d(TAG, "cycleCamera is still in progress.")
                return
            }
            cycleCameraInProgress = true
        }
        Log.d(TAG, "cycleCamera() enter")
        try {
            val nextCameraIndex = getNextSupportedCameraIndex()
            setCameraIndex(nextCameraIndex)

            val canSwapCamera = getCameraIndex() != -1
            // I think all devices *should* have at least one camera with
            // supported outputs, but adding this just in case.
            if (!canSwapCamera) {
                handleException(Camera2Exception("No cameras with supported outputs found"))
            } else {
                swapCamera(getCameraIndex())
            }
        } catch (exception: Exception) {
            handleException(exception)
        }
        Log.d(TAG, "cycleCamera() exit")
    }

    override fun getCameraIndex(): Int = cameraIndex

    private fun setCameraIndex(index: Int) {
        cameraIndex = index
    }

    @Synchronized
    override fun swapCamera(cameraId: Int) {
        Log.d(TAG, "swapCamera() enter. cameraState = $cameraState")

        val oldState = cameraState
        /* shutdown old camera but not the camera-callback thread */
        when (oldState) {
            CameraState.CAPTURE -> stopCapture()
            CameraState.ERROR, CameraState.CLOSED -> {
                //Previous camera open attempt failed.
                initCameraFrame()
                initCamera()
                startCapture()
            }
            CameraState.SETUP -> {
                // do nothing
            }
            else -> {
                // do nothing
            }
        }
        /* set camera ID */
        setCameraIndex(cameraId)
        executeAfterClosed = Runnable {
            when (oldState) {
                CameraState.CAPTURE -> {
                    initCameraFrame()
                    initCamera()
                    startCapture()
                }
                CameraState.SETUP -> {
                    // do nothing
                }
                else -> {
                    // do nothing
                }
            }
        }
        Log.d(TAG, "swapCamera() exit")
    }

    private fun isFrontCamera(): Boolean {
        return cameraInfoCache != null && cameraInfoCache!!.isFrontFacing()
    }

    private fun startCamThread() {
        Log.d(TAG, "startCamThread() enter")
        cameraThread = HandlerThread("Camera2VideoCapturer-Camera-Thread")
        cameraThread!!.start()
        cameraThreadHandler = Handler(cameraThread!!.looper)
        Log.d(TAG, "startCamThread() exit")
    }

    private fun stopCamThread() {
        Log.d(TAG, "stopCamThread() enter")
        try {
            cameraThread?.quitSafely()
            cameraThread?.join()
        } catch (exception: Exception) {
            handleException(exception)
        } finally {
            cameraThread = null
            cameraThreadHandler = null
        }
        Log.d(TAG, "stopCamThread() exit")
    }

    private fun selectCamera(lensDirection: Int): String? {
        for (id in cameraManager.cameraIdList) {
            val info = cameraManager.getCameraCharacteristics(id)
            /* discard cameras that don't face the right direction */
            if (lensDirection == info.get(CameraCharacteristics.LENS_FACING)) {
                Log.d(
                    TAG, "selectCamera() Direction the camera faces relative to device screen: " +
                            info.get(CameraCharacteristics.LENS_FACING)
                )
                return id
            }
        }
        return null
    }

    private fun selectCameraFpsRange(camId: String, fps: Int): Range<Int>? {
        for (id in cameraManager.cameraIdList) {
            if (id == camId) {
                val info = cameraManager.getCameraCharacteristics(id)
                val fpsLst = ArrayList<Range<Int>>()
                Collections.addAll(
                    fpsLst,
                    *info.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)!!
                )

                Log.d(TAG, "Supported fps ranges = $fpsLst")
                val selectedRange = Collections.min(fpsLst) { lhs, rhs ->
                    val calcError: (Range<Int>) -> Int = { range ->
                        Math.abs(range.lower - fps) + Math.abs(range.upper - fps)
                    }
                    calcError(lhs) - calcError(rhs)
                }
                Log.d(TAG, "Desired fps = $fps || Selected frame rate range = $selectedRange")
                return selectedRange
            }
        }
        return null
    }

    private fun findCameraIndex(camId: String?): Int {
        val idList = cameraManager.cameraIdList
        for (ndx in idList.indices) {
            if (idList[ndx] == camId) {
                return ndx
            }
        }
        return -1
    }

    private fun selectPreferredSize(camId: String, width: Int, height: Int): Size {
        val outputSizeArray = getCameraOutputSizes(camId)
        val sizeLst = ArrayList<Size>()
        Collections.addAll(sizeLst, *outputSizeArray)
        /* sort list by error from desired size */
        return Collections.min(sizeLst) { lhs, rhs ->
            val lXerror = abs(lhs.width - width)
            val lYerror = abs(lhs.height - height)
            val rXerror = abs(rhs.width - width)
            val rYerror = abs(rhs.height - height)
            (lXerror + lYerror) - (rXerror + rYerror)
        }
    }

    /*
     * Set current camera orientation
     */
    private fun calculateCamRotation(): Int {
        if (cameraInfoCache != null) {
            val cameraRotation = displayOrientationCache!!.getOrientation()
            val cameraOrientation = cameraInfoCache!!.sensorOrientation()
            return if (!cameraInfoCache!!.isFrontFacing()) {
                Math.abs((cameraRotation - cameraOrientation) % 360)
            } else {
                (cameraRotation + cameraOrientation + 360) % 360
            }
        } else {
            return 0
        }
    }

    private fun initCameraFrame() {
        if (getCameraIndex() == -1) {
            Log.d(TAG, " Camera Index cannot be -1. initCameraFrame() unsuccessful.")
            return
        }
        Log.d(TAG, "initCameraFrame() enter.")
        try {
            val cameraIdList = cameraManager.cameraIdList
            val camId = cameraIdList[getCameraIndex()]
            val preferredSize = selectPreferredSize(
                camId,
                frameDimensions.width,
                frameDimensions.height
            )
            cameraFrame?.close()
            cameraFrame = ImageReader.newInstance(
                preferredSize.width,
                preferredSize.height,
                PIXEL_FORMAT,
                3
            )
        } catch (exception: Exception) {
            handleException(exception)
        }

        Log.d(TAG, "initCameraFrame() exit.")
    }

    @SuppressLint("MissingPermission")
    private fun initCamera() {
        if (getCameraIndex() == -1) {
            Log.d(TAG, " Camera Index cannot be -1. initCamera() unsuccessful.")
            return
        }
        Log.d(TAG, "initCamera() enter.")
        try {
            cameraState = CameraState.SETUP
            // find desired camera & camera output size
            val cameraIdList = cameraManager.cameraIdList
            val camId = cameraIdList[getCameraIndex()]
            camFps = selectCameraFpsRange(camId, desiredFps)
            cameraFrame!!.setOnImageAvailableListener(frameObserver, cameraThreadHandler)
            cameraInfoCache = CameraInfoCache(cameraManager.getCameraCharacteristics(camId))
            cameraManager.openCamera(camId, cameraObserver, null)
        } catch (exception: Exception) {
            Log.d(TAG, "Camera cannot be opened. Check the error message below.")
            handleException(exception)
        }
        Log.d(TAG, "initCamera() exit.")
    }

    private fun handleException(exception: Exception) {
        cameraState = CameraState.ERROR
        synchronized(lock) {
            cycleCameraInProgress = false
        }
        //Log exception as an error
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        exception.printStackTrace(printWriter)
        printWriter.flush()
        val stackTrace = writer.toString()
        Log.d(TAG, stackTrace)

        //Send the exception to client
        onCaptureError(exception)
    }

    private fun startDisplayOrientationCache() {
        displayOrientationCache = DisplayOrientationCache(display, cameraThreadHandler!!)
    }

    private fun stopDisplayOrientationCache() {
        cameraThreadHandler?.let { handler ->
            displayOrientationCache?.let { cache ->
                handler.removeCallbacks(cache)
            }
        }
    }
}