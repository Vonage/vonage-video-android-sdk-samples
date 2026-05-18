package com.example.pictureinpicture

import android.text.TextUtils

// *** Fill the following variables using your own Project info  ***
// *** https://developer.vonage.com/en/video/getting-started     ***
object VonageVideoConfig {
    // Replace with your Vonage Video application ID
    const val APP_ID = "67b7059b-142b-4630-ab25-714e2ccd04fa"
    // Replace with your generated session ID
    const val SESSION_ID = "1_MX42N2I3MDU5Yi0xNDJiLTQ2MzAtYWIyNS03MTRlMmNjZDA0ZmF-fjE3NzkwNTQzMjY3NTR-SW1BcWN5emRqejVxMEZYMFh2SHVkcFgvfn5-"
    // Replace with your generated token
    const val TOKEN = "eyJhbGciOiJSUzI1NiIsImprdSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLWV1dzEucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvandrcyIsImtpZCI6IkNOPVZvbmFnZSAxdmFwaWd3IEludGVybmFsIENBOjo4NDczNTI3NzAwMTI4Nzg4MDkxNzA1NTg2NzcxNjU2NjEwMTY2OCIsInR5cCI6IkpXVCIsIng1dSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLWV1dzEucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvdjEvY2VydHMvMWYzZTI5Y2E3OWVjYWRlYTlmYjg5NzE4MDk1OTNmZWIifQ.eyJwcmluY2lwYWwiOnsiYWNsIjp7InBhdGhzIjp7Ii8qKiI6e319fSwidmlhbUlkIjp7ImVtYWlsIjoiYXJ0dXIub3NpbnNraUB2b25hZ2UuY29tIiwiZ2l2ZW5fbmFtZSI6IkFydCIsImZhbWlseV9uYW1lIjoiT3NpIiwicGhvbmVfbnVtYmVyIjoiNDg2MDE3OTkyNjEiLCJwaG9uZV9udW1iZXJfY291bnRyeSI6IlBMIiwib3JnYW5pemF0aW9uX2lkIjoiOTgxNDE0YTktMmZkNC00ZDE4LWIzN2ItNDhlMWQ5Y2EwMDdiIiwiYXV0aGVudGljYXRpb25NZXRob2RzIjpbeyJjb21wbGV0ZWRfYXQiOiIyMDI2LTA1LTE3VDE4OjE0OjE4LjU1NDg0MDAyOFoiLCJtZXRob2QiOiJpbnRlcm5hbCJ9XSwiaXBSaXNrIjp7ImlzX3Byb3h5Ijp0cnVlLCJpc192cG4iOnRydWUsInJpc2tfbGV2ZWwiOjc1fSwidG9rZW5UeXBlIjoidmlhbSIsImF1ZCI6InBvcnR1bnVzLmlkcC52b25hZ2UuY29tIiwiZXhwIjoxNzc5MDU0NjI2LCJqdGkiOiJmYzkzOWVmYi00YmEzLTQ5MDAtOTZmNC0yZmFhYWEwOWM1ZWUiLCJpYXQiOjE3NzkwNTQzMjYsImlzcyI6IlZJQU0tSUFQIiwibmJmIjoxNzc5MDU0MzExLCJzdWIiOiIyZDU5ZTBiNC02ZjQyLTQ2YTMtOGY0Ni01ZGM4MzljZWI1NzYifX0sImZlZGVyYXRlZEFzc2VydGlvbnMiOnsidmlkZW8tYXBpIjpbeyJhcGlLZXkiOiI1NTE5MTBlZiIsImFwcGxpY2F0aW9uSWQiOiI2N2I3MDU5Yi0xNDJiLTQ2MzAtYWIyNS03MTRlMmNjZDA0ZmEiLCJtYXN0ZXJBY2NvdW50SWQiOiI1NTE5MTBlZiIsImV4dHJhQ29uZmlnIjp7InZpZGVvLWFwaSI6eyJpbml0aWFsX2xheW91dF9jbGFzc19saXN0IjoiIiwicm9sZSI6Im1vZGVyYXRvciIsInNjb3BlIjoic2Vzc2lvbi5jb25uZWN0Iiwic2Vzc2lvbl9pZCI6IjFfTVg0Mk4ySTNNRFU1WWkweE5ESmlMVFEyTXpBdFlXSXlOUzAzTVRSbE1tTmpaREEwWm1GLWZqRTNOemt3TlRRek1qWTNOVFItU1cxQmNXTjVlbVJxZWpWeE1FWllNRmgyU0hWa2NGZ3ZmbjUtIn19fV19LCJhdWQiOiJwb3J0dW51cy5pZHAudm9uYWdlLmNvbSIsImV4cCI6MTc3OTA1NjE2NywianRpIjoiZDA2NmZhZTQtZDhjMS00ODc1LTg1ZjktMzI5ODQ0ZTZkNjM1IiwiaWF0IjoxNzc5MDU0MzY3LCJpc3MiOiJWSUFNLUlBUCIsIm5iZiI6MTc3OTA1NDM1Miwic3ViIjoiMmQ1OWUwYjQtNmY0Mi00NmEzLThmNDYtNWRjODM5Y2ViNTc2In0.xuG-h1szZY9ilc8OcqVs6cd2xZJ1zK6tpqvjrL5dTaZqg8ci9WKLlq4FTQEbPzE5qEhovpBcKRjGFrXgn4otGrk6EsKIc6A8dJoo38jHmZgP9mPKKhBFaMF6Km02z7KbF_u6m0b52P3fM7Cnak2QV9Q6KXdTfsYGLHsJZL3hNACQSYY4fMaKWFw2TMMF2xyPDA4y1pLlivZLj8Ui8BAokMf-v4OYGxp0RdAYtTgyHGJyt_SZUoPOTwiWcKiZ8kjQJg4ermLhsnzMgUCPiY4hhEhzFpGjjhVOyWQP0iY8-NqL-GeKsTESxN-FFjUcmgROdYQOVWc1oVPfQImNYeqo3w"
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