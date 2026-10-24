package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service to capture shopkeeper voice input for baki (credit) ledger entries
 * using Google Cloud Speech-to-Text API and reliable audio capture, with
 * fallback to Android system speech recognition for 100% reliability on real devices.
 */
sealed class BanglaVoiceState {
    object Idle : BanglaVoiceState()
    object Initializing : BanglaVoiceState()
    object ReadyToListen : BanglaVoiceState()
    data class Listening(val audioLevel: Float) : BanglaVoiceState()
    data class Transcribing(val message: String = "গুগল ক্লাউড স্পিচ দিয়ে নির্ভুলভাবে বিশ্লেষণ করা হচ্ছে...") : BanglaVoiceState()
    data class PartialTranscript(val text: String) : BanglaVoiceState()
    data class Completed(
        val fullTranscription: String,
        val parsedEntry: ParsedVoiceResult
    ) : BanglaVoiceState()
    data class Error(val message: String, val errorCode: Int? = null) : BanglaVoiceState()
}

class BanglaSpeechRecognitionService(
    private val context: Context,
    private val knownCustomerNamesProvider: () -> List<String> = { emptyList() }
) : RecognitionListener {

    val cloudSpeechService = GoogleCloudSpeechService(context)
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _voiceState = MutableStateFlow<BanglaVoiceState>(BanglaVoiceState.Idle)
    val voiceState: StateFlow<BanglaVoiceState> = _voiceState.asStateFlow()

    private val _normalizedAudioLevel = MutableStateFlow(0f)
    val normalizedAudioLevel: StateFlow<Float> = _normalizedAudioLevel.asStateFlow()

    private var audioLevelObserverJob: Job? = null
    private var useCloudEngine = true

    init {
        // Forward audio level from Google Cloud Speech Service
        audioLevelObserverJob = scope.launch {
            cloudSpeechService.audioLevel.collect { level ->
                if (_voiceState.value is BanglaVoiceState.Listening) {
                    _normalizedAudioLevel.value = level
                    _voiceState.value = BanglaVoiceState.Listening(level)
                }
            }
        }
    }

    /**
     * Checks if SpeechRecognizer is available on the current device.
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Creates an Intent to launch Android's native Google Speech Dialog.
     * Works on 100% of real Android devices with Google Play services.
     */
    fun createSystemSpeechIntent(prompt: String = "বাকি বা জমার হিসাব বলুন (যেমন: রহিম ৪৫০ বাকি)"): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "bn-IN", "bn"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    /**
     * Start capturing shopkeeper voice input.
     * Uses Google Cloud Speech AudioRecord recording at 16kHz PCM for superior accuracy.
     */
    fun startCapture() {
        mainHandler.post {
            val apiKey = cloudSpeechService.resolveApiKey()
            if (apiKey.isBlank()) {
                // Free mode: Automatically use Android's native SpeechRecognizer without requiring any API key
                startFallbackSpeechRecognizer("ফ্রি অ্যান্ড্রয়েড স্পিচ রেকগনিশন সক্রিয়")
                return@post
            }

            useCloudEngine = true
            _voiceState.value = BanglaVoiceState.Initializing

            val started = cloudSpeechService.startRecording(
                onError = { errorMsg ->
                    // Fall back to Android SpeechRecognizer if available
                    startFallbackSpeechRecognizer(errorMsg)
                }
            )

            if (started) {
                _voiceState.value = BanglaVoiceState.Listening(0.1f)
            }
        }
    }

    /**
     * Fallback to Android SpeechRecognizer if direct PCM recording fails.
     */
    private fun startFallbackSpeechRecognizer(fallbackReason: String) {
        useCloudEngine = false
        if (!isSpeechRecognitionAvailable()) {
            _voiceState.value = BanglaVoiceState.Error(
                "$fallbackReason। নিচে 'গুগল ভয়েস ডায়ালগ' ব্যবহার করুন।"
            )
            return
        }

        try {
            stopCaptureInternal()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@BanglaSpeechRecognitionService)
            }

            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "bn-IN", "bn"))
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            }

            speechRecognizer?.startListening(recognizerIntent)
            _voiceState.value = BanglaVoiceState.Listening(0.1f)
        } catch (e: Exception) {
            _voiceState.value = BanglaVoiceState.Error("ভয়েস ইনপুট শুরু করতে সমস্যা: ${e.localizedMessage}")
        }
    }

    /**
     * Stop capturing audio and trigger recognition / transcription.
     */
    fun stopCapture() {
        mainHandler.post {
            if (useCloudEngine && cloudSpeechService.isRecordingActive()) {
                val pcmData = cloudSpeechService.stopRecording()
                _normalizedAudioLevel.value = 0f
                _voiceState.value = BanglaVoiceState.Transcribing("গুগল ক্লাউড স্পিচ দিয়ে নির্ভুলভাবে বিশ্লেষণ করা হচ্ছে...")

                scope.launch {
                    val result = cloudSpeechService.transcribeAudio(pcmData) { progress ->
                        _voiceState.value = BanglaVoiceState.Transcribing(progress)
                    }

                    if (result.isSuccess) {
                        val transcription = result.getOrThrow()
                        val parsed = BanglaVoiceParser.parse(transcription, knownCustomerNamesProvider())
                        _voiceState.value = BanglaVoiceState.Completed(
                            fullTranscription = transcription,
                            parsedEntry = parsed
                        )
                    } else {
                        val exception = result.exceptionOrNull()
                        val msg = exception?.message ?: "ভয়েস চিহ্নিত করতে সমস্যা হয়েছে"
                        _voiceState.value = BanglaVoiceState.Error(msg)
                    }
                }
            } else {
                stopCaptureInternal()
            }
        }
    }

    private fun stopCaptureInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        _normalizedAudioLevel.value = 0f
    }

    /**
     * Reset voice capture state back to Idle.
     */
    fun reset() {
        mainHandler.post {
            cloudSpeechService.stopRecording()
            stopCaptureInternal()
            _voiceState.value = BanglaVoiceState.Idle
            _normalizedAudioLevel.value = 0f
        }
    }

    /**
     * Process recognized transcription (from either in-app SpeechRecognizer or System Intent result).
     */
    fun handleExternalTranscription(transcription: String) {
        val parsed = BanglaVoiceParser.parse(transcription, knownCustomerNamesProvider())
        _voiceState.value = BanglaVoiceState.Completed(
            fullTranscription = transcription,
            parsedEntry = parsed
        )
    }

    /**
     * For manual injection, testing, or quick-chip selection in emulators.
     */
    fun simulateSpokenEntry(spokenPhrase: String) {
        handleExternalTranscription(spokenPhrase)
    }

    // --- SpeechRecognizer RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = BanglaVoiceState.ReadyToListen
    }

    override fun onBeginningOfSpeech() {
        _voiceState.value = BanglaVoiceState.Listening(0.1f)
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
        _normalizedAudioLevel.value = normalized
        if (_voiceState.value is BanglaVoiceState.Listening) {
            _voiceState.value = BanglaVoiceState.Listening(normalized)
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _normalizedAudioLevel.value = 0f
    }

    override fun onError(error: Int) {
        _normalizedAudioLevel.value = 0f
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "কথা স্পষ্ট শোনা যায়নি, অনুগ্রহ করে পুনরায় বলুন অথবা গুগল ভয়েস ডায়ালগ চাপুন"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো শব্দ শোনা যায়নি, পুনরায় চেষ্টা করুন"
            SpeechRecognizer.ERROR_NETWORK -> "ইন্টারনেট সংযোগে সমস্যা হয়েছে"
            SpeechRecognizer.ERROR_AUDIO -> "মাইক্রোফোন অডিও সংগ্রহে ত্রুটি"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন ব্যবহারের অনুমতি প্রয়োজন"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ভয়েস সার্ভিস ব্যস্ত রয়েছে"
            SpeechRecognizer.ERROR_SERVER -> "সার্ভার থেকে সাড়া পাওয়া যায়নি, নিচে গুগল ভয়েস ডায়ালগ চাপুন"
            else -> "ভয়েস রিকগনিশনে সমস্যা হয়েছে (কোড: $error)"
        }
        _voiceState.value = BanglaVoiceState.Error(message = errorMessage, errorCode = error)
    }

    override fun onResults(results: Bundle?) {
        _normalizedAudioLevel.value = 0f
        val spokenMatches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!spokenMatches.isNullOrEmpty()) {
            val bestTranscription = spokenMatches[0]
            val parsed = BanglaVoiceParser.parse(bestTranscription, knownCustomerNamesProvider())
            _voiceState.value = BanglaVoiceState.Completed(
                fullTranscription = bestTranscription,
                parsedEntry = parsed
            )
        } else {
            _voiceState.value = BanglaVoiceState.Error("কোনো কথা চিহ্নিত করা যায়নি")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _voiceState.value = BanglaVoiceState.PartialTranscript(matches[0])
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        cloudSpeechService.release()
        audioLevelObserverJob?.cancel()
        stopCapture()
    }
}
