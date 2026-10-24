package com.example.voice

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class Transcribing(val message: String) : SpeechState()
    data class PartialResult(val text: String) : SpeechState()
    data class FinalResult(val text: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class VoiceSpeechManager(
    private val context: Context,
    knownCustomerNamesProvider: () -> List<String> = { emptyList() }
) : TextToSpeech.OnInitListener {

    val banglaRecognitionService = BanglaSpeechRecognitionService(context, knownCustomerNamesProvider)
    val hasApiKey: Boolean get() = banglaRecognitionService.cloudSpeechService.resolveApiKey().isNotBlank()
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    val audioLevel: StateFlow<Float> = banglaRecognitionService.normalizedAudioLevel

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {}

        // Forward state from BanglaSpeechRecognitionService
        scope.launch {
            banglaRecognitionService.voiceState.collect { voiceState ->
                when (voiceState) {
                    is BanglaVoiceState.Idle -> _speechState.value = SpeechState.Idle
                    is BanglaVoiceState.Initializing,
                    is BanglaVoiceState.ReadyToListen,
                    is BanglaVoiceState.Listening -> _speechState.value = SpeechState.Listening
                    is BanglaVoiceState.Transcribing -> _speechState.value = SpeechState.Transcribing(voiceState.message)
                    is BanglaVoiceState.PartialTranscript -> _speechState.value = SpeechState.PartialResult(voiceState.text)
                    is BanglaVoiceState.Completed -> _speechState.value = SpeechState.FinalResult(voiceState.fullTranscription)
                    is BanglaVoiceState.Error -> _speechState.value = SpeechState.Error(voiceState.message)
                }
            }
        }
    }

    fun startListening() {
        banglaRecognitionService.startCapture()
    }

    fun stopListening() {
        banglaRecognitionService.stopCapture()
    }

    fun simulatePhrase(phrase: String) {
        banglaRecognitionService.simulateSpokenEntry(phrase)
    }

    fun resetState() {
        banglaRecognitionService.reset()
        _speechState.value = SpeechState.Idle
    }

    fun cancelCapture() {
        stopListening()
        resetState()
    }

    fun restartListening() {
        stopListening()
        resetState()
        startListening()
    }

    fun createSystemSpeechIntent(prompt: String? = null): Intent {
        return if (prompt != null) {
            banglaRecognitionService.createSystemSpeechIntent(prompt)
        } else {
            banglaRecognitionService.createSystemSpeechIntent()
        }
    }

    fun speak(text: String, isBangla: Boolean = true) {
        if (!isTtsInitialized || textToSpeech == null) return
        try {
            val locale = if (isBangla) Locale.forLanguageTag("bn-BD") else Locale.US
            val result = textToSpeech?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.language = Locale.getDefault()
            }
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "BAKI_READ_BACK")
        } catch (_: Exception) {}
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            try {
                textToSpeech?.setLanguage(Locale.forLanguageTag("bn-BD"))
                textToSpeech?.setSpeechRate(0.95f)
            } catch (_: Exception) {}
        }
    }

    fun destroy() {
        stopSpeaking()
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
        textToSpeech = null
        banglaRecognitionService.destroy()
    }
}
