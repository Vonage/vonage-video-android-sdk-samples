package com.example.customaudiodriver

import android.text.TextUtils

// *** Fill the following variables using your own Project info  ***
// *** https://developer.vonage.com/en/video/getting-started     ***
object VonageVideoConfig {
    // Replace with your Vonage Video application ID
    const val APP_ID = "67b7059b-142b-4630-ab25-714e2ccd04fa"
    // Replace with your generated session ID
    const val SESSION_ID = "1_MX42N2I3MDU5Yi0xNDJiLTQ2MzAtYWIyNS03MTRlMmNjZDA0ZmF-fjE3NjYzNjIwMjczNTZ-UWtITVVuRGNTN1RUMEE5UTVGODJiVWlJfn5-"
    // Replace with your generated token
    const val TOKEN = "eyJhbGciOiJSUzI1NiIsImprdSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLWV1dzEucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvandrcyIsImtpZCI6IkNOPVZvbmFnZSAxdmFwaWd3IEludGVybmFsIENBOjo5Mjk0NDE2NDY2MDQ3MDkxNjg2ODM2NzE2NDUyNzgyODQyOTU5NyIsInR5cCI6IkpXVCIsIng1dSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLWV1dzEucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvdjEvY2VydHMvODJhMmI0OWQzZDA0OWVhNjFmMWNkNmVmMjJkNWE5ZDUifQ.eyJwcmluY2lwYWwiOnsiYWNsIjp7InBhdGhzIjp7Ii8qKiI6e319fSwidmlhbUlkIjp7ImVtYWlsIjoiYXJ0dXIub3NpbnNraUB2b25hZ2UuY29tIiwiZ2l2ZW5fbmFtZSI6IkFydCIsImZhbWlseV9uYW1lIjoiT3NpIiwicGhvbmVfbnVtYmVyIjoiNDg2MDE3OTkyNjEiLCJwaG9uZV9udW1iZXJfY291bnRyeSI6IlBMIiwib3JnYW5pemF0aW9uX2lkIjoiOTgxNDE0YTktMmZkNC00ZDE4LWIzN2ItNDhlMWQ5Y2EwMDdiIiwiYXV0aGVudGljYXRpb25NZXRob2RzIjpbeyJjb21wbGV0ZWRfYXQiOiIyMDI1LTEyLTIyVDAwOjA0OjEzLjQwNDE1NDIxMVoiLCJtZXRob2QiOiJpbnRlcm5hbCJ9XSwiaXBSaXNrIjp7ImlzX3Byb3h5Ijp0cnVlLCJyaXNrX2xldmVsIjo4NH0sInRva2VuVHlwZSI6InZpYW0iLCJhdWQiOiJwb3J0dW51cy5pZHAudm9uYWdlLmNvbSIsImV4cCI6MTc2NjM2MjMyNywianRpIjoiNTgyNTI1MWUtZDcwNy00ZTM2LWJiMWUtZGJhZWVhN2M4MDY5IiwiaWF0IjoxNzY2MzYyMDI3LCJpc3MiOiJWSUFNLUlBUCIsIm5iZiI6MTc2NjM2MjAxMiwic3ViIjoiMmQ1OWUwYjQtNmY0Mi00NmEzLThmNDYtNWRjODM5Y2ViNTc2In19LCJmZWRlcmF0ZWRBc3NlcnRpb25zIjp7InZpZGVvLWFwaSI6W3siYXBpS2V5IjoiNTUxOTEwZWYiLCJhcHBsaWNhdGlvbklkIjoiNjdiNzA1OWItMTQyYi00NjMwLWFiMjUtNzE0ZTJjY2QwNGZhIiwibWFzdGVyQWNjb3VudElkIjoiNTUxOTEwZWYiLCJleHRyYUNvbmZpZyI6eyJ2aWRlby1hcGkiOnsiaW5pdGlhbF9sYXlvdXRfY2xhc3NfbGlzdCI6IiIsInJvbGUiOiJtb2RlcmF0b3IiLCJzY29wZSI6InNlc3Npb24uY29ubmVjdCIsInNlc3Npb25faWQiOiIxX01YNDJOMkkzTURVNVlpMHhOREppTFRRMk16QXRZV0l5TlMwM01UUmxNbU5qWkRBMFptRi1makUzTmpZek5qSXdNamN6TlRaLVVXdElUVlZ1UkdOVE4xUlVNRUU1VVRWR09ESmlWV2xKZm41LSJ9fX1dfSwiYXVkIjoicG9ydHVudXMuaWRwLnZvbmFnZS5jb20iLCJleHAiOjE3NjYzNjM5MTcsImp0aSI6ImJlMjQ3YzczLTVmYmEtNDUxMS1iMWVjLWJhOThiZDNkZTNmZiIsImlhdCI6MTc2NjM2MjExNywiaXNzIjoiVklBTS1JQVAiLCJuYmYiOjE3NjYzNjIxMDIsInN1YiI6IjJkNTllMGI0LTZmNDItNDZhMy04ZjQ2LTVkYzgzOWNlYjU3NiJ9.OZDfzAyS_kje62f18mqLAJ-jflYewriW8wLF1Iz7atynHLb7wU4D6dXN850lykwjs5_J4WWkycJ-ylpXRL6mabjyWjhfHDftfnxJ9zkKXo_BH7Mjo_p9rqXXeh36VUdCVjuogW1AhN4bH3AFfCqbsy8YEiE6LQG92eejOlsKqiRJkOxllpXIoGPvJ01kSnHyt1tEaAOIHRk1u_4AULhAjqKGg2Jrgod2LdcpK_cH_yvL9DBZweimZjK03SLl8msVInST-52yzMCv3RJb78oQHGEPawTJsdBPbiMJgxYDy52ktuIbHvq_pUYK65DmFK_Eu3pWToLQDOdAvXn8x39G2Q"

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