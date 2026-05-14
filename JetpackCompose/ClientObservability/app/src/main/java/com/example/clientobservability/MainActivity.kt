package com.example.clientobservability

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.Session
import com.opentok.android.Stream
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit
import com.opentok.android.SubscriberKit.SubscriberVideoStats
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tokbox.sample.clientobservability.network.APIService
import com.tokbox.sample.clientobservability.network.GetSessionResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {

    private lateinit var publisherViewContainer: ViewGroup
    private lateinit var subscriberViewContainer: ViewGroup

    private var latestVideoStats by mutableStateOf<SubscriberVideoStats?>(null)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var retrofit: Retrofit? = null
    private var apiService: APIService? = null

    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null

    private var sessionConfigRequested = false

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
            publisherViewContainer.addView(publisher?.view)
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
                session.subscribe(subscriber)
                subscriberViewContainer.addView(subscriber?.view)
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(TAG, "onStreamDropped: Stream Dropped: ${stream.streamId} in session: ${session.sessionId}")
            if (subscriber != null) {
                subscriber = null
                subscriberViewContainer.removeAllViews()
                latestVideoStats = null
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
        Log.d(TAG, "onVideoStats: Data received")
        Log.d(
            TAG,
            "onVideoStats: Sender Stats connectionEstimatedBandwidth" +
                (stats.senderStats?.connectionEstimatedBandwidth ?: "NULL"),
        )
        Log.d(
            TAG,
            "onVideoStats: Sender Stats connectionMaxAllocatedBitrate" +
                (stats.senderStats?.connectionMaxAllocatedBitrate ?: "NULL"),
        )
        Log.d(TAG, "onVideoStats: videoBytesReceived${stats.videoBytesReceived}")
        Log.d(TAG, "onVideoStats: timeStamp${stats.timeStamp}")
        Log.d(TAG, "onVideoStats: videoPacketsLost${stats.videoPacketsLost}")
        Log.d(TAG, "onVideoStats: videoPacketsReceived${stats.videoPacketsReceived}")
        mainHandler.post { latestVideoStats = stats }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                VideoChatPermissionWrapper(
                    onPermissionsGranted = { startSessionConfigFlow() },
                ) {
                    VideoCallScreen(
                        onOpenTokContainersReady = ::onOpenTokContainersReady,
                        videoStats = latestVideoStats,
                    )
                }
            }
        }
    }

    private fun onOpenTokContainersReady(subscriber: View, publisher: View) {
        subscriberViewContainer = subscriber as ViewGroup
        publisherViewContainer = publisher as ViewGroup
    }

    override fun onPause() {
        super.onPause()
        session?.onPause()
    }

    override fun onResume() {
        super.onResume()
        session?.onResume()
    }

    private fun startSessionConfigFlow() {
        if (sessionConfigRequested || session != null) return
        sessionConfigRequested = true
        if (ServerConfig.hasChatServerUrl()) {
            if (!ServerConfig.isValid()) {
                finishWithMessage("Invalid chat server url: ${ServerConfig.CHAT_SERVER_URL}")
                return
            }
            initRetrofit()
            getSession()
        } else {
            if (!OpenTokConfig.isValid()) {
                finishWithMessage("Invalid OpenTokConfig. ${OpenTokConfig.getDescription()}")
                return
            }
            initializeSession(OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN)
        }
    }

    private fun getSession() {
        Log.i(TAG, "getSession")
        val service = apiService ?: return
        service.getSession().enqueue(object : Callback<GetSessionResponse> {
            override fun onResponse(call: Call<GetSessionResponse>, response: Response<GetSessionResponse>) {
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    runOnUiThread {
                        finishWithMessage("getSession failed: HTTP ${response.code()}")
                    }
                    return
                }
                runOnUiThread {
                    initializeSession(body.apiKey, body.sessionId, body.token)
                }
            }

            override fun onFailure(call: Call<GetSessionResponse>, t: Throwable) {
                runOnUiThread { throw RuntimeException(t.message, t) }
            }
        })
    }

    private fun initializeSession(apiKey: String, sessionId: String, token: String) {
        Log.i(TAG, "apiKey: $apiKey")
        Log.i(TAG, "sessionId: $sessionId")
        Log.i(TAG, "token: $token")
        session = Session.Builder(this, apiKey, sessionId).build()
        session?.setSessionListener(sessionListener)
        session?.connect(token)
    }

    private fun initRetrofit() {
        val logging = HttpLoggingInterceptor().apply { level = Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        retrofit = Retrofit.Builder()
            .baseUrl(ServerConfig.CHAT_SERVER_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
        apiService = retrofit!!.create(APIService::class.java)
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
