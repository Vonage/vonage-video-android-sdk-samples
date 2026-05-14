package com.example.clientobservability

import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.opentok.android.SubscriberKit.SubscriberVideoStats

@Composable
fun VideoCallScreen(
    onOpenTokContainersReady: (subscriberContainer: View, publisherContainer: View) -> Unit,
    videoStats: SubscriberVideoStats? = null,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = modifier.fillMaxSize()) {
            val density = LocalDensity.current
            AndroidView(
                factory = { ctx ->
                    val root = FrameLayout(ctx)
                    val subscriberFrame = FrameLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                    }
                    val marginPx = with(density) { 16.dp.roundToPx() }
                    val wPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        90f,
                        ctx.resources.displayMetrics,
                    ).toInt()
                    val hPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        120f,
                        ctx.resources.displayMetrics,
                    ).toInt()
                    val publisherFrame = FrameLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(wPx, hPx).apply {
                            gravity = Gravity.BOTTOM or Gravity.END
                            bottomMargin = marginPx
                            marginEnd = marginPx
                        }
                        setBackgroundColor(Color.parseColor("#CCCCCC"))
                        setPadding(2, 2, 2, 2)
                    }
                    root.addView(subscriberFrame)
                    root.addView(publisherFrame)
                    onOpenTokContainersReady(subscriberFrame, publisherFrame)
                    root
                },
                modifier = Modifier.fillMaxSize(),
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
