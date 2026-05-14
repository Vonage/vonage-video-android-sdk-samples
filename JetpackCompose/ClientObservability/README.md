# Client Observability (Kotlin + Jetpack Compose)

This sample is a Kotlin/Jetpack Compose port of the legacy `Client-Observability-Java` sample.
It shows how to retrieve runtime statistics from the Vonage Video Android SDK at the
session/publisher/subscriber level and display them live in the UI.

It reuses the same building blocks as the `CustomAudioDriver` sample:

* [`VonageVideoConfig`](app/src/main/java/com/example/clientobservability/VonageVideoConfig.kt)
  — hard-coded `APP_ID` / `SESSION_ID` / `TOKEN` with simple validation.
* [`VideoChatPermissionWrapper`](app/src/main/java/com/example/clientobservability/VideoChatPermissionWrapper.kt)
  — Composable gatekeeper that requests `CAMERA` and `RECORD_AUDIO` before rendering the call.

## What the sample demonstrates

### Enable sender statistics on the publisher

`senderStatsTrack(true)` must be set on the `Publisher.Builder` so that the **subscriber's**
`VideoStatsListener` callback can later expose estimated sender stats
(`connectionEstimatedBandwidth`, `connectionMaxAllocatedBitrate`). This is off by default.

```kotlin
publisher = Publisher.Builder(this)
    .senderStatsTrack(true)
    .build()
```

### Listen to subscriber video stats

```kotlin
subscriber = Subscriber.Builder(this, stream).build().apply {
    setSubscriberListener(subscriberListener)
    setVideoStatsListener(videoStatsListener)
    session?.subscribe(this)
}

private val videoStatsListener = SubscriberKit.VideoStatsListener { _, stats ->
    Log.d(TAG, "estBandwidth=${stats.senderStats?.connectionEstimatedBandwidth}")
    Log.d(TAG, "maxBitrate=${stats.senderStats?.connectionMaxAllocatedBitrate}")
    Log.d(TAG, "bytesRecv=${stats.videoBytesReceived}")
    Log.d(TAG, "pktsLost=${stats.videoPacketsLost}")
    Log.d(TAG, "pktsRecv=${stats.videoPacketsReceived}")
}
```

The latest `SubscriberVideoStats` is hoisted into Compose state and rendered as a small
overlay (top-right) by `VideoCallScreen`.

## Configure

Edit
[`VonageVideoConfig.kt`](app/src/main/java/com/example/clientobservability/VonageVideoConfig.kt)
and fill in `APP_ID`, `SESSION_ID`, `TOKEN`. See the
[Vonage Video getting started guide](https://developer.vonage.com/en/video/getting-started)
for how to obtain those values.

## Run

Open the project in Android Studio (Hedgehog or later) and run the `app` configuration on a
physical device. Two participants must join the same session — the publisher uploads with
`senderStatsTrack(true)` enabled, the subscriber receives video stats every reporting interval.

## Migration notes vs. `Client-Observability-Java`

| Java version                                 | Kotlin/Compose version                            |
| -------------------------------------------- | ------------------------------------------------- |
| `AppCompatActivity` + XML layouts            | `ComponentActivity` + Jetpack Compose             |
| `EasyPermissions`                            | `accompanist-permissions` + `VideoChatPermissionWrapper` |
| `OpenTokConfig` / `ServerConfig` + Retrofit  | `VonageVideoConfig` (hard-coded only)             |
| `findViewById` + `FrameLayout` containers    | `AndroidView` interop inside a `Box`              |
| Stats logged to Logcat                       | Logged to Logcat **and** drawn as a Compose overlay |

The Retrofit/Moshi/OkHttp server-side credential fetching from the original sample was
intentionally dropped to keep this port focused; if you need it, the original Java sample
in `JetpackCompose/Client-Observability-Java/` still demonstrates that flow.
