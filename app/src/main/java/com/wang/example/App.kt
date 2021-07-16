package com.wang.example

import android.app.Application
import android.widget.Toast
import com.wang.example.App.Companion.app

class App : Application() {
    companion object {
        lateinit var app: App
    }

    override fun onCreate() {
        super.onCreate()
        app = this;
    }
}

fun getApp(): Application {
    return app
}

fun CharSequence.toast() {
    Toast.makeText(getApp(), this, Toast.LENGTH_SHORT).show()
}
