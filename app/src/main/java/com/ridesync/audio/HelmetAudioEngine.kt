package com.ridesync.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class HelmetAudioEngine(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language US not supported for TTS")
            } else {
                configureBluetoothAudioAttributes()
                isInitialized = true
                Log.d(TAG, "Helmet TTS Audio Engine Initialized successfully")
            }
        } else {
            Log.e(TAG, "TTS Initialization failed with status: $status")
        }
    }

    private fun configureBluetoothAudioAttributes() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        tts?.setAudioAttributes(attributes)
    }

    fun speakAlert(text: String) {
        if (!isInitialized) {
            Log.w(TAG, "TTS requested before initialization complete")
            return
        }
        val utteranceId = "RideSync_Alert_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    fun announceRiderDrop(riderName: String, distanceKm: Double) {
        val formattedDist = String.format(Locale.US, "%.1f", distanceKm)
        val message = "$riderName is $formattedDist kilometers behind the pack"
        speakAlert(message)
    }

    fun announceStopEvent(riderName: String, stopReason: String) {
        val message = "$riderName has stopped for $stopReason"
        speakAlert(message)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "HelmetAudioEngine"
    }
}
