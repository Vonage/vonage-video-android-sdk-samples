package com.example.clientobservability

import com.opentok.android.SubscriberKit.SubscriberVideoStats

/**
 * Combined subscriber observability metrics for the UI overlay.
 * Video receive stats come from [SubscriberVideoStats]; bandwidth metrics come from
 * [SubscriberMediaLinkStats].
 */
data class ObservabilityStats(
    val videoBytesReceived: Int = 0,
    val videoPacketsLost: Int = 0,
    val videoPacketsReceived: Int = 0,
    val timeStamp: Double = 0.0,
    val localEstimatedBandwidth: Long? = null,
    val remoteEstimatedBandwidth: Long? = null,
    val networkDegradationSource: Int? = null,
) {
    companion object {
        fun fromVideoStats(
            stats: SubscriberVideoStats,
            mediaLink: MediaLinkSnapshot? = null,
        ): ObservabilityStats = ObservabilityStats(
            videoBytesReceived = stats.videoBytesReceived,
            videoPacketsLost = stats.videoPacketsLost,
            videoPacketsReceived = stats.videoPacketsReceived,
            timeStamp = stats.timeStamp,
            localEstimatedBandwidth = mediaLink?.localEstimatedBandwidth,
            remoteEstimatedBandwidth = mediaLink?.remoteEstimatedBandwidth,
            networkDegradationSource = mediaLink?.networkDegradationSource,
        )
    }
}

data class MediaLinkSnapshot(
    val localEstimatedBandwidth: Long?,
    val remoteEstimatedBandwidth: Long?,
    val networkDegradationSource: Int?,
)
