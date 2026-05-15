package com.example.clientobservability

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.opentok.android.SubscriberKit.SubscriberVideoStats

@Composable
fun VideoCallScreen(
    subscriberView: View?,
    publisherView: View?,
    videoStats: SubscriberVideoStats? = null,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { container ->
                    container.setSingleChild(subscriberView)
                },
                modifier = Modifier.fillMaxSize(),
            )
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        setBackgroundColor(Color.parseColor("#CCCCCC"))
                        setPadding(2, 2, 2, 2)
                    }
                },
                update = { container ->
                    container.setSingleChild(publisherView)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(90.dp, 120.dp),
            )
            if (videoStats != null) {
                StatsOverlay(
                    stats = videoStats,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                )
            }
        }
    }
}

private fun ViewGroup.setSingleChild(view: View?) {
    removeAllViews()
    if (view == null) return
    (view.parent as? ViewGroup)?.removeView(view)
    addView(
        view,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )
}

@Composable
private fun StatsOverlay(
    stats: SubscriberVideoStats,
    modifier: Modifier = Modifier,
) {
    val senderEstBandwidth = stats.senderStats?.connectionEstimatedBandwidth?.toString() ?: "N/A"
    val senderMaxBitrate = stats.senderStats?.connectionMaxAllocatedBitrate?.toString() ?: "N/A"

    Column(
        modifier = modifier
            .background(ComposeColor(0xAA000000))
            .padding(8.dp),
    ) {
        StatRow("Subscriber video stats")
        StatRow("estBandwidth", senderEstBandwidth)
        StatRow("maxBitrate", senderMaxBitrate)
        StatRow("bytesRecv", stats.videoBytesReceived.toString())
        StatRow("pktsLost", stats.videoPacketsLost.toString())
        StatRow("pktsRecv", stats.videoPacketsReceived.toString())
        StatRow("timestamp", stats.timeStamp.toString())
    }
}

@Composable
private fun StatRow(label: String, value: String? = null) {
    val text = if (value == null) label else "$label: $value"
    Text(
        text = text,
        color = ComposeColor.White,
        fontSize = 11.sp,
    )
}
