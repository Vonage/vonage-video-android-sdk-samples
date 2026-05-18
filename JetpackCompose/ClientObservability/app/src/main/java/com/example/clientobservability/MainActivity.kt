package com.example.clientobservability

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.clientobservability.VonageVideoConfig.description
import com.example.clientobservability.VonageVideoConfig.isValid
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.Session
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit

class MainActivity : ComponentActivity() {

    private var publisherView by mutableStateOf<View?>(null)
    private var subscriberView by mutableStateOf<View?>(null)
    private var latestObservabilityStats by mutableStateOf<ObservabilityStats?>(null)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var latestMediaLinkSnapshot: MediaLinkSnapshot? = null

    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null

    private val publisherListener = object : PublisherKit.PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamCreated: Publisher Stream Created. Own stream ${stream.streamId}")
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.d(TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream ${stream.streamId}")
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            finishWithMessage("PublisherKit onError: ${opentokError.message}")
        }
    }

    private val sessionListener = object : Session.SessionListener {
        override fun onConnected(session: Session) {
            Log.d(TAG, "onConnected: Connected to session: ${session.sessionId}")

            publisher = Publisher.Builder(this@MainActivity).senderStatsTrack(true).build()
            publisher?.setPublisherListener(publisherListener)
            publisher?.renderer?.setStyle(
                BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL,
            )
            publisherView = publisher?.view
            if (publisher?.view is GLSurfaceView) {
                (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
            }
            session.publish(publisher)
        }

        override fun onDisconnected(session: Session) {
            Log.d(TAG, "onDisconnected: Disconnected from session: ${session.sessionId}")
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamReceived: New Stream Received ${stream.streamId} in session: ${session.sessionId}")
            if (subscriber == null) {
                subscriber = Subscriber.Builder(this@MainActivity, stream).build()
                subscriber?.renderer?.setStyle(
                    BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL,
                )
                subscriber?.setSubscriberListener(subscriberListener)
                subscriber?.setVideoStatsListener(videoStatsListener)
                subscriber?.setMediaLinkStatsListener(mediaLinkStatsListener)
                session.subscribe(subscriber)
                subscriberView = subscriber?.view
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamDropped: Stream Dropped: ${stream.streamId} in session: ${session.sessionId}")
            if (subscriber != null) {
                subscriber = null
                subscriberView = null
                latestObservabilityStats = null
                latestMediaLinkSnapshot = null
            }
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            finishWithMessage("Session error: ${opentokError.message}")
        }
    }

    private val subscriberListener = object : SubscriberKit.SubscriberListener {
        override fun onConnected(subscriberKit: SubscriberKit) {
            Log.d(TAG, "onConnected: Subscriber connected. Stream: ${subscriberKit.stream.streamId}")
        }

        override fun onDisconnected(subscriberKit: SubscriberKit) {
            Log.d(TAG, "onDisconnected: Subscriber disconnected. Stream: ${subscriberKit.stream.streamId}")
        }

        override fun onError(subscriberKit: SubscriberKit, opentokError: OpentokError) {
            finishWithMessage("SubscriberKit onError: ${opentokError.message}")
        }
    }

    private val videoStatsListener = SubscriberKit.VideoStatsListener { _, stats ->
        Log.d(TAG, "onVideoStats: videoBytesReceived=${stats.videoBytesReceived}")
        Log.d(TAG, "onVideoStats: timeStamp=${stats.timeStamp}")
        Log.d(TAG, "onVideoStats: videoPacketsLost=${stats.videoPacketsLost}")
        Log.d(TAG, "onVideoStats: videoPacketsReceived=${stats.videoPacketsReceived}")
        mainHandler.post {
            latestObservabilityStats = ObservabilityStats.fromVideoStats(stats, latestMediaLinkSnapshot)
        }
    }

    private val mediaLinkStatsListener = object : SubscriberKit.MediaLinkStatsListener {
        override fun onMediaLinkStats(
            subscriber: SubscriberKit,
            mediaLinkStats: SubscriberKit.SubscriberMediaLinkStats,
        ) {
            val localBandwidth = mediaLinkStats.transport?.connectionEstimatedBandwidth
            val remoteBandwidth = mediaLinkStats.remotePublisherTransport?.connectionEstimatedBandwidth
            Log.d(TAG, "onMediaLinkStats: localEstimatedBandwidth=$localBandwidth")
            Log.d(TAG, "onMediaLinkStats: remoteEstimatedBandwidth=$remoteBandwidth")
            Log.d(TAG, "onMediaLinkStats: degradationSource=${mediaLinkStats.networkDegradationSource}")
            mainHandler.post {
                latestMediaLinkSnapshot = MediaLinkSnapshot(
                    localEstimatedBandwidth = localBandwidth,
                    remoteEstimatedBandwidth = remoteBandwidth,
                    networkDegradationSource = mediaLinkStats.networkDegradationSource,
                )
                latestObservabilityStats?.let { current ->
                    latestObservabilityStats = current.copy(
                        localEstimatedBandwidth = localBandwidth,
                        remoteEstimatedBandwidth = remoteBandwidth,
                        networkDegradationSource = mediaLinkStats.networkDegradationSource,
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isValid) {
            finishWithMessage("Invalid VonageVideoConfig. $description")
            return
        }

        setContent {
            MaterialTheme {
                VideoChatPermissionWrapper(
                    onPermissionsGranted = { connectToSession() },
                ) {
                    VideoCallScreen(
                        subscriberView = subscriberView,
                        publisherView = publisherView,
                        observabilityStats = latestObservabilityStats,
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        session?.onPause()
    }

    override fun onResume() {
        super.onResume()
        session?.onResume()
    }

    private fun connectToSession() {
        if (session != null) return
        session = Session.Builder(
            this,
            VonageVideoConfig.APP_ID,
            VonageVideoConfig.SESSION_ID,
        ).build()
        session?.setSessionListener(sessionListener)
        session?.connect(VonageVideoConfig.TOKEN)
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
