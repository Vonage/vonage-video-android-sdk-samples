package com.example.multiparty

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun SimpleMultipartyScreen(
    publisherAudioEnabled: Boolean,
    publisherVideoEnabled: Boolean,
    subscriberVisible: List<Boolean>,
    subscriberAudioEnabled: List<Boolean>,
    onSwapCamera: () -> Unit,
    onPublisherAudioChanged: (Boolean) -> Unit,
    onPublisherVideoChanged: (Boolean) -> Unit,
    onSubscriberAudioChanged: (index: Int, enabled: Boolean) -> Unit,
    onPublisherContainerReady: (FrameLayout) -> Unit,
    onSubscriberContainerReady: (index: Int, FrameLayout) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SubscriberCell(
                        modifier = Modifier.weight(1f),
                        visible = subscriberVisible.getOrNull(0) == true,
                        audioEnabled = subscriberAudioEnabled.getOrNull(0) != false,
                        onAudioChanged = { onSubscriberAudioChanged(0, it) },
                        onContainerReady = { onSubscriberContainerReady(0, it) },
                    )
                    SubscriberCell(
                        modifier = Modifier.weight(1f),
                        visible = subscriberVisible.getOrNull(1) == true,
                        audioEnabled = subscriberAudioEnabled.getOrNull(1) != false,
                        onAudioChanged = { onSubscriberAudioChanged(1, it) },
                        onContainerReady = { onSubscriberContainerReady(1, it) },
                    )
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SubscriberCell(
                        modifier = Modifier.weight(1f),
                        visible = subscriberVisible.getOrNull(2) == true,
                        audioEnabled = subscriberAudioEnabled.getOrNull(2) != false,
                        onAudioChanged = { onSubscriberAudioChanged(2, it) },
                        onContainerReady = { onSubscriberContainerReady(2, it) },
                    )
                    SubscriberCell(
                        modifier = Modifier.weight(1f),
                        visible = subscriberVisible.getOrNull(3) == true,
                        audioEnabled = subscriberAudioEnabled.getOrNull(3) != false,
                        onAudioChanged = { onSubscriberAudioChanged(3, it) },
                        onContainerReady = { onSubscriberContainerReady(3, it) },
                    )
                }
            }

            ControlsRow(
                publisherAudioEnabled = publisherAudioEnabled,
                publisherVideoEnabled = publisherVideoEnabled,
                onSwapCamera = onSwapCamera,
                onPublisherAudioChanged = onPublisherAudioChanged,
                onPublisherVideoChanged = onPublisherVideoChanged,
            )
        }

        AndroidView(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(width = 150.dp, height = 200.dp)
                .background(Color.Black),
            factory = {
                FrameLayout(it).also(onPublisherContainerReady)
            }
        )
    }
}

@Composable
private fun SubscriberCell(
    modifier: Modifier,
    visible: Boolean,
    audioEnabled: Boolean,
    onAudioChanged: (Boolean) -> Unit,
    onContainerReady: (FrameLayout) -> Unit,
) {
    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                FrameLayout(ctx).also(onContainerReady)
            }
        )

        if (visible) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.size(8.dp))
                Switch(checked = audioEnabled, onCheckedChange = onAudioChanged)
            }
        }
    }
}

@Composable
private fun ControlsRow(
    publisherAudioEnabled: Boolean,
    publisherVideoEnabled: Boolean,
    onSwapCamera: () -> Unit,
    onPublisherAudioChanged: (Boolean) -> Unit,
    onPublisherVideoChanged: (Boolean) -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onSwapCamera) {
                Text("Swap Camera")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Audio")
                Spacer(modifier = Modifier.size(6.dp))
                Switch(checked = publisherAudioEnabled, onCheckedChange = onPublisherAudioChanged)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Video")
                Spacer(modifier = Modifier.size(6.dp))
                Switch(checked = publisherVideoEnabled, onCheckedChange = onPublisherVideoChanged)
            }
        }
    }
}
