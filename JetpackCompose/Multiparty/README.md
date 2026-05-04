# Simple Multiparty

This app shows how to implement a simple video call application with several clients using Kotlin and Jetpack Compose. It demonstrates how to enable/disable audio and video as well as swap the camera.

---

## Subscribing to Multiple Streams

The signaling sample subscribed to only one stream. In a multiparty video/audio call, there are multiple streams.

```kotlin
override fun onStreamReceived(session: Session, stream: Stream) {
    val subscriber = Subscriber.Builder(this@MainActivity, stream).build()
    session.subscribe(subscriber)
    addSubscriber(subscriber)
}
````

This simple multiparty app is able to handle a maximum of four subscribers. Once a new stream is received, the `MainActivity` class creates a new `Subscriber` object and subscribes the `Session` object to it. The subscriber stream is then rendered to the screen.

```kotlin
private val maxSubscribers = 4
```

---

## Adding User Interface Controls (Jetpack Compose)

This sample shows how you can add user interface controls for the following:

* Turning a publisher's audio stream on and off
* Turning a publisher's video stream on and off
* Swapping the publisher's camera

---

### Toggle Audio

```kotlin
// onCreate
onPublisherAudioChanged = { enabled ->
    publisherAudioEnabled.value = enabled
    publisher?.publishAudio = enabled
}
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
