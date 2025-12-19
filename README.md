# Vonage Android SDK Samples

_Check documentation to understand [Video API overview](https://developer.vonage.com/en/video/overview). See instructions below in order to [open Android project](#open-project) and [obtain Vonage credentials](#obtaining-vonage-credentials)._

The Android projects in this directory demonstrate typical use cases and features available in the [Vonage Android SDK](https://developer.vonage.com/en/video/client-sdks/android/overview):

## What's Inside

You can find various sample apps written with `Jetpack Compose` & `XML` based layout, that demonstrate the Vonage Android SDK features:

### Jetpack Compose
- **Basic-Video-Renderer** demonstrates how to create a custom video renderer

### XML layout
- **Basic-Video-Chat** demonstrates how to publish and subscribe to streams in a session. Best place to start
- **Multiparty-Constraint-Layout** demonstrates how to use [ConstraintLayout](https://developer.android.com/training/constraint-layout) to position all the videos in a multiparty session
- **Archiving** demonstrates how to utilize Vonage server to start, stop, and playback session recordings
- **Basic-Audio-Driver** demonstrates how to publish a random audio signal and save audio streams to the file.
- **Advanced-Audio-Driver** demonstrates how to create a more advanced custom audio driver
- **Live-Photo-Capture** demonstrates how to capture an image from a subscribed video stream
- **Webview-Screen-Sharing** demonstrates how to publish a webview-screen-sharing video, using the WebView as the source
- **Phone-Call-Detection** demonstrates how to detect incoming and outgoing phone calls
- **Video-Transformers** demonstrates how to use pre-built transformers in the Vonage Media Processor library or create your own custom video transformer to apply to published video.
- **E2EE-Video-Chat** demonstrates how to have a two-way End to End Encrypted (E2EE) audio and video communication using Vonage.
- **Basic-Video-Chat-With-ForegroundServices** demonstrates how to setup foreground services in order to have a seamless user experience.
- **Camera-Controls** demonstrates how to set the preferred torch/flashlight mode and zoom factor for the camera.
- **Basic-Video-Chat-ConnectionService** shows how to use `ConnectionService` integration in Kotlin.

## Open project

1. Clone this repository `git@github.com:vonage/vonage-video-android-sdk-samples.git`
2. Start [Android Studio](https://developer.android.com/studio)
3. In the `Quick Start` panel, click `Open an existing Android Studio Project`
4. Navigate to the repository folder, select the desired project subfolder, and click `Choose`

## Obtaining Vonage Credentials

[Step by step tutorial](https://developer.vonage.com/en/video/getting-started)

To use the Vonage platform you need a session ID, token, and APP ID.
You can get these values by creating a project in your [Vonage Dashboard](https://dashboard.vonage.com/), using the project Tools. For production deployment, you must generate the
session ID and token values using one of the [Vonage Server
SDKs](https://developer.vonage.com/en/video/server-sdks/overview).

#### Obtaining OpenTok Credentials

To use the OpenTok platform you need a session ID, token, and API key.
You can get these values by creating a project on your [OpenTok Account
Page](https://tokbox.com/account/) and scrolling down to the Project Tools
section of your Project page. For production deployment, you must generate the
session ID and token values using one of the [OpenTok Server
SDKs](https://tokbox.com/developer/sdks/server/).

## Development and Contributing

Feel free to copy and modify the source code herein for your projects. Please consider sharing your modifications with us, especially if they might benefit other developers using the Vonage Android SDK. See the [License](LICENSE) for more information.

Interested in contributing? You :heart: pull requests! See the 
[Contribution](CONTRIBUTING.md) guidelines.

## Getting Help

We love to hear from you so if you have questions, comments or find a bug in the project, let us know! You can either:

- Open an issue on this repository
- See [Vonage support](https://api.support.vonage.com/) for support options
- Tweet at us! We're [@VonageDev](https://twitter.com/VonageDev) on Twitter
- Or [join the Vonage Developer Community Slack](https://developer.nexmo.com/community/slack)

