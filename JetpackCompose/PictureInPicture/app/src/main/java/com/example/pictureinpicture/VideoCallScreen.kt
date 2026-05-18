package com.example.pictureinpicture

import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoCallScreen(
    isInPipMode: Boolean,
    onContainersReady: (subscriber: FrameLayout, publisher: FrameLayout) -> Unit,
    onEnterPictureInPicture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VideoContainers(onContainersReady = onContainersReady)

            if (!isInPipMode) {
                Button(
                    onClick = onEnterPictureInPicture,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    Text("Enter Picture-In-Picture")
                }
            }
        }
    }
}

@Composable
private fun VideoContainers(
    onContainersReady: (subscriber: FrameLayout, publisher: FrameLayout) -> Unit,
) {
    val density = LocalDensity.current
    AndroidView(
        factory = { ctx ->
            val root = FrameLayout(ctx)
            val subscriberFrame = FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(ctx.getColor(R.color.container_bg))
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
                setBackgroundColor(ctx.getColor(R.color.publisher_border_color))
                setPadding(2, 2, 2, 2)
            }
            root.addView(subscriberFrame)
            root.addView(publisherFrame)
            onContainersReady(subscriberFrame, publisherFrame)
            root
        },
        modifier = Modifier.fillMaxSize(),
    )
}
