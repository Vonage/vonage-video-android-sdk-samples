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

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Audio")
    Spacer(modifier = Modifier.size(6.dp))
    Switch(checked = publisherAudioEnabled, onCheckedChange = onPublisherAudioChanged)
}
```

The `onPublisherAudioChanged` function toggles its audio on or off.

---

### Toggle Video

```kotlin
// onCreate
onPublisherVideoChanged = { enabled ->
    publisherVideoEnabled.value = enabled
    publisher?.publishVideo = enabled
}
```

```kotlin
@Composable
Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Video")
    Spacer(modifier = Modifier.size(6.dp))
    Switch(checked = publisherVideoEnabled, onCheckedChange = onPublisherVideoChanged)
}
```

The `onPublisherVideoChanged` function toggles its video on or off.

---

### Swap Camera

```kotlin
// onCreate
onSwapCamera = { publisher?.cycleCamera() }
```

```kotlin
Button(onClick = onSwapCamera) {
    Text("Swap Camera")
}
```

The `cycleCamera()` method of a `Publisher` object switches to the next available camera on the device.

---

## Further Reading

* Review [Basic Video Capturer](../BasicVideoCapturer) project
* Review [other sample projects](../)
* Read more about [Vonage Android SDK](https://developer.vonage.com/en/video/client-sdks/android/overview)
