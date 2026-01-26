# Custom Video Capturing

This tutorial walks through the steps required to make modifications to the video capturer in your Vonage Android application.

In this example, the app uses a custom video capturer to mirror a video image. This is done simply to illustrate the basic principals of setting up a custom video capturer.

`MirrorVideoCapturer` is a custom class that extends the `BaseVideoCapturer` class (defined in the `Vonage Android SDK`). The `BaseVideoCapturer` class lets you define a custom video capturer to be used by an `Vonage` publisher:
    
```kotlin
publisher = Publisher.Builder(this@MainActivity)
                .capturer(
                    MirrorVideoCapturer(
                        this@MainActivity,
                        Publisher.CameraCaptureResolution.HIGH,
                        Publisher.CameraCaptureFrameRate.FPS_30
                    )
                )
                .build()
```

The getCaptureSettings() method provides settings used by the custom video capturer:

```kotlin
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
```

The `BaseVideoCapturer.CaptureSetting` class (which defines the capturerSettings property) is defined by the `Vonage Android SDK`. In this sample code, the format of the video capturer is set to use `NV21` as the pixel format, with a specific number of frames per second, a specific height, and a specific width.

The `BaseVideoCapturer.startCapture()` method is called when a publisher starts capturing video to be sent as a stream to the Vonage session. This will occur after the `Session.publish(publisher)` method is called:

```kotlin
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
```

## Further Reading

* Review [Basic Video Capturer](../BasicVideoCapturer) project
* Review [other sample projects](../)
* Read more about [Vonage Android SDK](https://developer.vonage.com/en/video/client-sdks/android/overview)
