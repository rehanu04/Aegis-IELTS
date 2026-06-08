package com.aegis.ielts

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point required by Hilt.
 * Must be declared in AndroidManifest.xml via android:name=".AegisApplication".
 */
@HiltAndroidApp
class AegisApplication : Application()
