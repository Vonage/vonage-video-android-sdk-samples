package com.example.multiparty

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.multiparty.VonageVideoConfig.isValid
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.Session
import com.opentok.android.Stream
import com.opentok.android.Subscriber


class MainActivity : ComponentActivity() {

    private val maxSubscribers = 4

    private var session: Session? = null
    private var publisher: Publisher? = null

    private var sessionConnected = false

    private var publisherContainer: FrameLayout? = null
    private val subscriberContainers: Array<FrameLayout?> = arrayOfNulls(maxSubscribers)
    private val subscribers: Array<Subscriber?> = arrayOfNulls(maxSubscribers)
    private val subscriberStreamIds: Array<String?> = arrayOfNulls(maxSubscribers)

    private val publisherAudioEnabled = mutableStateOf(true)
    private val publisherVideoEnabled = mutableStateOf(true)
    private val subscriberVisible = mutableStateListOf<Boolean>().apply { repeat(maxSubscribers) { add(false) } }
    private val subscriberAudioEnabled = mutableStateListOf<Boolean>().apply { repeat(maxSubscribers) { add(true) } }

    private val publisherListener = object : PublisherKit.PublisherListener {
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

    private val sessionListener = object : Session.SessionListener {
        override fun onConnected(session: Session) {
            Log.d(TAG, "onConnected: Connected to session ${session.sessionId}")
            sessionConnected = true

            val publisher = Publisher.Builder(this@MainActivity).build().also {
                it.setPublisherListener(publisherListener)
                it.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
            }
            this@MainActivity.publisher = publisher

            runOnUiThread {
                publisherContainer?.removeAllViews()
                publisherContainer?.addView(publisher.view)
            }

            session.publish(publisher)
        }

        override fun onDisconnected(session: Session) {
            Log.d(TAG, "onDisconnected: disconnected from session ${session.sessionId}")
            sessionConnected = false
            this@MainActivity.session = null
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            finishWithMessage("Session error: ${opentokError.message}")
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamReceived: New stream ${stream.streamId} in session ${session.sessionId}")

            val subscriber = Subscriber.Builder(this@MainActivity, stream).build()
            session.subscribe(subscriber)
            addSubscriber(subscriber)
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamDropped: Stream ${stream.streamId} dropped from session ${session.sessionId}")
            removeSubscriberWithStream(stream)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isValid) {
            finishWithMessage("Invalid VonageVideoConfig. ${VonageVideoConfig.description}")
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VideoChatPermissionWrapper(
                        onPermissionsGranted = {
                            initSession()
                        }
                    ) {
                        SimpleMultipartyScreen(
                            publisherAudioEnabled = publisherAudioEnabled.value,
                            publisherVideoEnabled = publisherVideoEnabled.value,
                            maxSubscribers = maxSubscribers,
                            subscriberVisible = subscriberVisible,
                            subscriberAudioEnabled = subscriberAudioEnabled,
                            onSwapCamera = { publisher?.cycleCamera() },
                            onPublisherAudioChanged = { enabled ->
                                publisherAudioEnabled.value = enabled
                                publisher?.publishAudio = enabled
                            },
                            onPublisherVideoChanged = { enabled ->
                                publisherVideoEnabled.value = enabled
                                publisher?.publishVideo = enabled
                            },
                            onSubscriberAudioChanged = { index, enabled ->
                                subscriberAudioEnabled[index] = enabled
                                subscribers.getOrNull(index)?.subscribeToAudio = enabled
                            },
                            onPublisherContainerReady = { container ->
                                publisherContainer = container
                            },
                            onSubscriberContainerReady = { index, container ->
                                subscriberContainers[index] = container
                            },
                        )
                    }
                }
            }
        }
    }

    private fun initSession() {
        if (session != null) return

        Log.d(TAG, "Permissions granted. Initializing Session.")

        val session = Session.Builder(this, VonageVideoConfig.APP_ID, VonageVideoConfig.SESSION_ID).build()
        this.session = session
        session.setSessionListener(sessionListener)
        session.connect(VonageVideoConfig.TOKEN)
    }

    override fun onResume() {
        super.onResume()
        session?.onResume()
    }

    override fun onPause() {
        super.onPause()
        session?.onPause()
        if (isFinishing) {
            disconnectSession()
        }
    }

    override fun onDestroy() {
        disconnectSession()
        super.onDestroy()
    }

    private fun addSubscriber(subscriber: Subscriber) {
        val index = subscribers.indexOfFirst { it == null }
        if (index == -1) {
            Toast.makeText(
                this,
                "New subscriber ignored, maxSubscribers limit reached",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        subscribers[index] = subscriber
        subscriberStreamIds[index] = subscriber.stream.streamId

        subscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
        subscriber.subscribeToAudio = true

        runOnUiThread {
            subscriberContainers[index]?.removeAllViews()
            subscriberContainers[index]?.addView(subscriber.view)
        }

        subscriberAudioEnabled[index] = true
        subscriberVisible[index] = true
    }

    private fun removeSubscriberWithStream(stream: Stream) {
        val index = subscriberStreamIds.indexOfFirst { it == stream.streamId }
        if (index == -1) return

        val subscriber = subscribers[index] ?: return

        runOnUiThread {
            subscriberContainers[index]?.removeView(subscriber.view)
        }

        subscribers[index] = null
        subscriberStreamIds[index] = null
        subscriberVisible[index] = false
        subscriberAudioEnabled[index] = true
    }

    private fun disconnectSession() {
        val session = session ?: return
        if (!sessionConnected) return

        sessionConnected = false

        for (subscriber in subscribers) {
            if (subscriber != null) {
                session.unsubscribe(subscriber)
            }
        }

        publisher?.let { publisher ->
            runOnUiThread { publisherContainer?.removeView(publisher.view) }
            session.unpublish(publisher)
        }
        publisher = null

        session.disconnect()
    }

    private fun finishWithMessage(message: String) {
        Log.e(TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}