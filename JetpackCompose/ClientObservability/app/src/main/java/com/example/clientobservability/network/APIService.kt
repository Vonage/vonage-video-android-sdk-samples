package com.tokbox.sample.clientobservability.network

import retrofit2.Call
import retrofit2.http.GET

interface APIService {
    @GET("session")
    fun getSession(): Call<GetSessionResponse>
}
