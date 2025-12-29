package com.example.customaudiodriver

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoCallScreen(
    subscriberView: View?,
    publisherView: View?
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Subscriber view (top-left)
        if (subscriberView != null) {
            AndroidView(
                factory = { context ->
                    val container = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    container.addView(subscriberView)
                    container
                },
                update = { container ->
                    // Remove old views and add new one if view changed
                    container.removeAllViews()
                    subscriberView?.let { container.addView(it) }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }

        // Publisher view (bottom-right)
        if (publisherView != null) {
            AndroidView(
                factory = { context ->
                    val container = RelativeLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    container.addView(publisherView)
                    container
                },
                update = { container ->
                    // Remove old views and add new one if view changed
                    container.removeAllViews()
                    publisherView?.let { container.addView(it) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(150.dp, 200.dp)
            )
        }
    }
}

