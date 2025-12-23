package com.example.customaudiodriver

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.customaudiodriver.VonageVideoConfig.description
import com.example.customaudiodriver.VonageVideoConfig.isValid
import com.opentok.android.AudioDeviceManager
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.PublisherKit.PublisherListener
import com.opentok.android.Session
import com.opentok.android.Session.SessionListener
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit
import com.opentok.android.SubscriberKit.VideoListener

class MainActivity : ComponentActivity() {

    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null

    private var publisherView: View? by mutableStateOf(null)
    private var subscriberView: View? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isValid) {
            finishWithMessage("Check your VonageVideoConfig properties. $description")
            return
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VideoChatPermissionWrapper(
                        onPermissionsGranted = {
                            initSession()
                        }
                    ) {
                        VideoCallScreen(
                            subscriberView = subscriberView,
                            publisherView = publisherView
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        session?.onPause()
        if (isFinishing) {
            disconnectSession()
        }
    }

    override fun onResume() {
        super.onResume()
        session?.onResume()
    }

    override fun onDestroy() {
        disconnectSession()
        super.onDestroy()
    }

    private fun initSession() {
        if (session != null) return

        Log.d(TAG, "Permissions granted. Initializing Session and Audio Device.")

        /*
           CRITICAL: The Custom Audio Device must be set BEFORE
           the Session object is instantiated.
        */
        val noiseAudioDevice = NoiseAudioDevice(this)
        AudioDeviceManager.setAudioDevice(noiseAudioDevice)

        session = Session.Builder(this, VonageVideoConfig.APP_ID, VonageVideoConfig.SESSION_ID).build().apply {
            setSessionListener(sessionListener)
            connect(VonageVideoConfig.TOKEN)
        }
    }

    private val publisherListener: PublisherListener = object : PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamCreated: Own stream ${stream.streamId} created")
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamDestroyed: Own stream ${stream.streamId} destroyed")
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            finishWithMessage("PublisherKit error: ${opentokError.message}")
        }
    }

    private val sessionListener: SessionListener = object : SessionListener {
        override fun onConnected(session: Session) {
            Log.d(TAG, "onConnected: Connected to session ${session.sessionId}")
            runOnUiThread {
                publisher = Publisher.Builder(this@MainActivity).build().apply {
                    setPublisherListener(publisherListener)
                    setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
                }

                publisherView = publisher?.view
                (publisher?.view as? GLSurfaceView)?.setZOrderOnTop(true)

                session.publish(publisher)
            }
        }

        override fun onDisconnected(session: Session) {
            Log.d(TAG, "onDisconnected: disconnected from session ${session.sessionId}")
            this@MainActivity.session = null
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            finishWithMessage("Session error: ${opentokError.message}")
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamReceived: New stream ${stream.streamId}")
            if (subscriber != null) return
            subscribeToStream(stream)
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamDropped: Stream ${stream.streamId}")
            if (subscriber?.stream?.streamId == stream.streamId) {
                runOnUiThread {
                    subscriberView = null
                    subscriber = null
                }
            }
        }
    }

    private val videoListener: VideoListener = object : VideoListener {
        override fun onVideoDataReceived(subscriberKit: SubscriberKit) {
            runOnUiThread {
                subscriber?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
                subscriberView = subscriber?.view
            }
        }
        override fun onVideoDisabled(subscriberKit: SubscriberKit, s: String) {}
        override fun onVideoEnabled(subscriberKit: SubscriberKit, s: String) {}
        override fun onVideoDisableWarning(subscriberKit: SubscriberKit) {}
        override fun onVideoDisableWarningLifted(subscriberKit: SubscriberKit) {}
    }

    // --- Helpers ---
    private fun subscribeToStream(stream: Stream) {
        subscriber = Subscriber.Builder(this, stream).build().apply {
            setVideoListener(videoListener)
            session?.subscribe(this)
        }
    }

    private fun disconnectSession() {
        subscriber?.let {
            subscriberView = null
            session?.unsubscribe(it)
            subscriber = null
        }
        publisher?.let {
            publisherView = null
            session?.unpublish(it)
            publisher = null
        }
        session?.disconnect()
        session = null
    }

    private fun finishWithMessage(message: String) {
        Log.e(TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}