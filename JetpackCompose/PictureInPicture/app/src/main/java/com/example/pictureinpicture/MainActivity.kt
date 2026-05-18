package com.example.pictureinpicture

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.Session
import com.opentok.android.Stream
import com.opentok.android.Subscriber

class MainActivity : ComponentActivity() {

    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null

    private lateinit var subscriberViewContainer: FrameLayout
    private lateinit var publisherViewContainer: FrameLayout

    private var permissionsGranted = false
    private var containersReady = false
    private var isInPipMode by mutableStateOf(false)

    private val sessionListener = object : Session.SessionListener {
        override fun onConnected(session: Session) {
            Log.d(TAG, "Session connected")

            if (publisher == null) {
                publisher = Publisher.Builder(applicationContext).build()
                session.publish(publisher)

                publisherViewContainer.addView(publisher?.view)
                if (publisher?.view is GLSurfaceView) {
                    (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
                }
            }
        }

        override fun onDisconnected(session: Session) {
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            if (subscriber == null) {
                subscriber = Subscriber.Builder(applicationContext, stream).build()
                session.subscribe(subscriber)
                subscriberViewContainer.addView(subscriber?.view)
            } else {
                Log.d(TAG, "This sample supports just one subscriber")
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            subscriberViewContainer.removeAllViews()
            subscriber = null
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            finishWithMessage("Session error: ${opentokError.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!VonageVideoConfig.isValid) {
            finishWithMessage("Invalid VonageVideoConfig. ${VonageVideoConfig.description}")
            return
        }

        setContent {
            MaterialTheme {
                VideoChatPermissionWrapper(
                    onPermissionsGranted = {
                        permissionsGranted = true
                        tryStartSession()
                    },
                ) {
                    VideoCallScreen(
                        isInPipMode = isInPipMode,
                        onContainersReady = { subscriberFrame, publisherFrame ->
                            subscriberViewContainer = subscriberFrame
                            publisherViewContainer = publisherFrame
                            if (!containersReady) {
                                containersReady = true
                                tryStartSession()
                            }
                        },
                        onEnterPictureInPicture = ::enterPictureInPicture,
                    )
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            publisherViewContainer.visibility = android.view.View.GONE
            publisher?.view?.visibility = android.view.View.GONE
        } else {
            publisherViewContainer.visibility = android.view.View.VISIBLE
            publisher?.view?.visibility = android.view.View.VISIBLE
            if (publisher?.view is GLSurfaceView) {
                (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (!isInPictureInPictureMode) {
            session?.onPause()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isInPictureInPictureMode) {
            session?.onResume()
        }
    }

    override fun onStop() {
        super.onStop()

        subscriber?.let { subscriberViewContainer.removeView(it.view) }
        publisher?.let { publisherViewContainer.removeView(it.view) }
    }

    private fun tryStartSession() {
        if (!permissionsGranted || !containersReady || session != null) return

        session = Session.Builder(
            applicationContext,
            VonageVideoConfig.APP_ID,
            VonageVideoConfig.SESSION_ID,
        ).build()
        session?.setSessionListener(sessionListener)
        session?.connect(VonageVideoConfig.TOKEN)
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, "Picture-in-picture is not supported on this device.", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            Toast.makeText(this, "Picture-in-picture is not supported on this device.", Toast.LENGTH_SHORT)
                .show()
            return
        }
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))
                .build()
            enterPictureInPictureMode(params)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to enter picture-in-picture mode", e)
            Toast.makeText(this, "Could not enter picture-in-picture mode.", Toast.LENGTH_SHORT).show()
        }
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
