package com.ridesync

import android.app.Application
import com.google.firebase.FirebaseApp

class RideSyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
