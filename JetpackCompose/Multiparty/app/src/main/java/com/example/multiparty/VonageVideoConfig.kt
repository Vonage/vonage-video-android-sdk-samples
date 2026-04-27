package com.example.multiparty

import android.text.TextUtils

object VonageVideoConfig {
    // Replace with your Vonage Video APP ID
    const val APP_ID = ""
    // Replace with your generated session ID
    const val SESSION_ID = ""
    // Replace with your generated token
    const val TOKEN = ""

    // *** The code below is to validate this configuration file. You do not need to modify it  ***
    val isValid: Boolean
        get() = !(TextUtils.isEmpty(com.example.multiparty.VonageVideoConfig.APP_ID) || TextUtils.isEmpty(
            com.example.multiparty.VonageVideoConfig.SESSION_ID
        ) || TextUtils.isEmpty(com.example.multiparty.VonageVideoConfig.TOKEN))
    val description: String
        get() = """
               VonageVideoConfig:
               APP_ID: ${com.example.multiparty.VonageVideoConfig.APP_ID}
               SESSION_ID: ${com.example.multiparty.VonageVideoConfig.SESSION_ID}
               TOKEN: ${com.example.multiparty.VonageVideoConfig.TOKEN}
               """.trimIndent()
}