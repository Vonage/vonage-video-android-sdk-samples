package com.example.clientobservability

import android.text.TextUtils

// *** Fill the following variables using your own Project info  ***
// *** https://developer.vonage.com/en/video/getting-started     ***
object VonageVideoConfig {
    // Replace with your Vonage Video application ID
    const val APP_ID = ""
    // Replace with your generated session ID
    const val SESSION_ID = ""
    // Replace with your generated token
    const val TOKEN = ""

    // *** The code below is to validate this configuration file. You do not need to modify it  ***
    val isValid: Boolean
        get() = !(TextUtils.isEmpty(APP_ID) || TextUtils.isEmpty(SESSION_ID) || TextUtils.isEmpty(TOKEN))
    val description: String
        get() = """
               VonageVideoConfig:
               APP_ID: $APP_ID
               SESSION_ID: $SESSION_ID
               TOKEN: $TOKEN
               """.trimIndent()

}