package com.radioprep

import android.app.Application
import android.content.Context

class RadioPrepApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as RadioPrepApp).container
