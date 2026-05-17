package com.example.clientobservability.network

import com.squareup.moshi.Json

data class GetSessionResponse(
    @Json(name = "apiKey") val apiKey: String,
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "token") val token: String,
)
