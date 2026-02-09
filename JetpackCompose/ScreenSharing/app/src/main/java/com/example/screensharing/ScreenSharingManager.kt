package com.example.screensharing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

class ScreenSharingManager(private val context: Context) {

    private var service: ScreenSharingService? = null
    var currentState: State = State.UNBIND_SERVICE
        private set

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            val localBinder = binder as ScreenSharingService.LocalBinder
            service = localBinder.getService()
            currentState = State.BIND_SERVICE
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    init {
        bindService()
    }

    private fun bindService() {
        val intent = Intent(context, ScreenSharingService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun startForeground() {
        service?.startForeground()
        currentState = State.START_FOREGROUND
    }

    fun endForeground() {
        service?.endForeground()
        currentState = State.END_FOREGROUND
    }

    fun unbindService() {
        context.unbindService(connection)
        currentState = State.UNBIND_SERVICE
    }

    enum class State {
        BIND_SERVICE,
        START_FOREGROUND,
        END_FOREGROUND,
        UNBIND_SERVICE
    }
}
